package com.pdfconverter.service;

import com.pdfconverter.config.StorageConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Handles conversions between PDF, Word (.docx) and PowerPoint (.pptx).
 *
 * This version delegates all conversion work to LibreOffice running in
 * headless mode. LibreOffice performs real layout analysis internally
 * (text blocks, tables, images, fonts), so output preserves formatting
 * far more faithfully than a pure PDFBox/POI text-extraction approach.
 *
 * FIX (export filter): --convert-to receives an explicit filter name
 * (e.g. "docx:MS Word 2007 XML") instead of a bare extension. On
 * stripped-down LibreOffice installs, a bare "docx"/"pptx" target does
 * not always resolve to a registered export filter when importing from
 * PDF, which previously produced:
 *   "Error: no export filter for ... found, aborting."
 *
 * SPEED FIX: instead of creating a brand-new LibreOffice user profile
 * directory (via -env:UserInstallation=...) and deleting it on every
 * single request, we now maintain a small fixed-size POOL of profile
 * directories, one per allowed concurrent conversion slot. Each profile
 * is created once and reused across requests. Creating a fresh profile
 * from scratch forces LibreOffice to re-initialize its user config
 * (registrymodifications.xcu, cache, etc.) on every call, which is a
 * meaningful chunk of the per-conversion latency. Reusing a warm
 * profile removes that repeated cost. Concurrency safety is preserved
 * because each pooled profile is only ever used by one LibreOffice
 * process at a time (enforced by the blocking queue below), so there is
 * no risk of two processes fighting over the same profile lock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final StorageConfig storageConfig;

    private static final int MAX_CONCURRENT_CONVERSIONS = 3;
    private static final long CONVERSION_TIMEOUT_SECONDS = 120;
    private static final long SLOT_WAIT_TIMEOUT_SECONDS = 120;

    @Value("${libreoffice.binary-path:#{null}}")
    private String configuredLibreOfficeBinary;

    // Pool of pre-warmed profile directories. Size == MAX_CONCURRENT_CONVERSIONS.
    // A BlockingQueue doubles as both the "permit" mechanism (acquiring a slot
    // blocks if none are free) and the pool of reusable profile paths, so we
    // no longer need a separate Semaphore.
    private BlockingQueue<Path> profilePool;

    @PostConstruct
    private void initProfilePool() {
        try {
            Path tempDir = storageConfig.tempPath();
            Files.createDirectories(tempDir);

            profilePool = new ArrayBlockingQueue<>(MAX_CONCURRENT_CONVERSIONS);
            for (int i = 0; i < MAX_CONCURRENT_CONVERSIONS; i++) {
                Path profileDir = tempDir.resolve("lo_profile_slot_" + i);
                Files.createDirectories(profileDir);
                profilePool.put(profileDir);
            }
            log.info("Initialized {} pre-warmed LibreOffice profile slots", MAX_CONCURRENT_CONVERSIONS);
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Failed to initialize LibreOffice profile pool", e);
        }
    }

    private String resolveLibreOfficeBinary() {
        // 1. Check if explicitly configured via application properties or environment variable mapping
        if (configuredLibreOfficeBinary != null && !configuredLibreOfficeBinary.isBlank()) {
            return configuredLibreOfficeBinary.trim();
        }

        // 2. Check if running on Windows
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            return "soffice.exe";
        }

        // 3. For Linux containers (like Render), check standard absolute paths explicitly
        File standardBin = new File("/usr/bin/soffice");
        if (standardBin.exists()) {
            return "/usr/bin/soffice";
        }

        File altBin = new File("/usr/bin/libreoffice");
        if (altBin.exists()) {
            return "/usr/bin/libreoffice";
        }

        // 4. Final fallback
        return "soffice";
    }

    public enum ConversionType {
        PDF_TO_WORD, WORD_TO_PDF, PDF_TO_PPT, PPT_TO_PDF
    }

    public record ConversionResult(File file, String fileName) {}

    public ConversionResult convert(MultipartFile file, ConversionType type) throws IOException {
        Path uploadDir = storageConfig.uploadPath();
        Path tempDir = storageConfig.tempPath();

        String uid = UUID.randomUUID().toString();
        String originalName = sanitizeFileName(file.getOriginalFilename());
        String baseName = stripExtension(originalName);

        Path inputPath = uploadDir.resolve(uid + "_" + originalName);
        file.transferTo(inputPath);

        return switch (type) {
            case WORD_TO_PDF -> {
                File converted = convertWithLibreOffice(inputPath, tempDir, uid, "pdf");
                yield new ConversionResult(converted, baseName + ".pdf");
            }
            case PPT_TO_PDF -> {
                File converted = convertWithLibreOffice(inputPath, tempDir, uid, "pdf");
                yield new ConversionResult(converted, baseName + ".pdf");
            }
            case PDF_TO_WORD -> {
                File converted = convertWithLibreOffice(inputPath, tempDir, uid, "docx");
                yield new ConversionResult(converted, baseName + ".docx");
            }
            case PDF_TO_PPT -> {
                File converted = convertWithLibreOffice(inputPath, tempDir, uid, "pptx");
                yield new ConversionResult(converted, baseName + ".pptx");
            }
        };
    }

    private File convertWithLibreOffice(Path inputPath, Path outputDir, String uid, String targetFormat)
            throws IOException {
        Files.createDirectories(outputDir);

        // Borrow a pre-warmed profile directory from the pool. This blocks if
        // all slots are currently in use, which also caps concurrency at
        // MAX_CONCURRENT_CONVERSIONS (same role the old Semaphore played).
        Path profileDir;
        try {
            profileDir = profilePool.poll(SLOT_WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for a free conversion slot", e);
        }
        if (profileDir == null) {
            throw new IOException("Server busy: all conversion slots in use, please retry shortly");
        }

        try {
            String binaryPath = resolveLibreOfficeBinary();
            log.info("Executing LibreOffice binary: {} (profile slot: {})", binaryPath, profileDir);

            String inputFileName = inputPath.getFileName().toString().toLowerCase();

            // Use an explicit export filter name (not just the bare extension).
            // Bare extensions like "docx" or "pptx" are ambiguous and, on many
            // headless/minimal LibreOffice installs, fail to resolve to a
            // registered export filter, especially for PDF -> Office conversions.
            String filter = resolveExportFilter(targetFormat, inputFileName);
            log.info("Using LibreOffice export filter: {}", filter);

            // Build process arguments dynamically to support PDF input filters correctly
            List<String> command = new ArrayList<>();
            command.add(binaryPath);
            command.add("--headless");
            command.add("--norestore");
            command.add("--nolockcheck");
            command.add("--nologo");
            command.add("--nodefault");
            command.add("-env:UserInstallation=" + profileDir.toUri());

            // Explicitly force LibreOffice to use the Writer PDF import filter if input is a PDF
            if (inputFileName.endsWith(".pdf")) {
                command.add("--infilter=writer_pdf_import");
            }

            command.add("--convert-to");
            command.add(filter);
            command.add("--outdir");
            command.add(outputDir.toString());
            command.add(inputPath.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                }
            }

            boolean finished;
            try {
                finished = process.waitFor(CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Conversion interrupted", e);
            }

            if (!finished) {
                process.destroyForcibly();
                throw new IOException("LibreOffice conversion timed out after "
                        + CONVERSION_TIMEOUT_SECONDS + "s");
            }

            if (process.exitValue() != 0) {
                log.warn("LibreOffice exited with code {} for file {}", process.exitValue(), inputPath);
                throw new IOException("LibreOffice conversion failed (exit " + process.exitValue()
                        + "):\n" + processOutput);
            }

            // LibreOffice names the output file using the extension portion of
            // the filter string (before any ':'), so strip that off when
            // looking for the produced file.
            String targetExtension = filter.contains(":") ? filter.substring(0, filter.indexOf(':')) : filter;

            String inputBaseName = stripExtension(inputPath.getFileName().toString());
            Path libreOfficeOutput = outputDir.resolve(inputBaseName + "." + targetExtension);

            if (!Files.exists(libreOfficeOutput)) {
                throw new IOException("Expected LibreOffice output not found: " + libreOfficeOutput
                        + "\nLibreOffice log:\n" + processOutput);
            }

            File outFile = outputDir.resolve(uid + "_output." + targetExtension).toFile();
            Files.move(libreOfficeOutput, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return outFile;

        } finally {
            // Return the profile slot to the pool for reuse instead of deleting
            // it. We intentionally do NOT wipe the directory here, that's what
            // makes subsequent conversions faster.
            try {
                profilePool.put(profileDir);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while returning profile slot {} to pool", profileDir);
            }

            // Clean up the uploaded input file, it's no longer needed after conversion.
            try {
                Files.deleteIfExists(inputPath);
            } catch (IOException e) {
                log.warn("Failed to delete input file {}: {}", inputPath, e.getMessage());
            }
        }
    }

    /**
     * Maps a target format + source file to an explicit LibreOffice export
     * filter string suitable for --convert-to. Using explicit filter names
     * (rather than bare extensions) avoids "no export filter" errors that
     * occur on some headless/minimal LibreOffice builds, particularly for
     * PDF -> DOCX/PPTX conversions.
     */
    private String resolveExportFilter(String targetFormat, String inputFileName) {
        boolean fromPdf = inputFileName.endsWith(".pdf");

        return switch (targetFormat.toLowerCase()) {
            case "pdf" -> "pdf"; // bare "pdf" resolves fine for Writer/Impress -> PDF export
            case "docx" -> fromPdf ? "docx:MS Word 2007 XML" : "docx";
            case "pptx" -> fromPdf ? "pptx:Impress MS PowerPoint 2007 XML" : "pptx";
            default -> targetFormat.toLowerCase();
        };
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "file";
        return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String stripExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) return fileName;
        return fileName.substring(0, lastDot);
    }
}