package com.pdfconverter.service;

import com.pdfconverter.config.StorageConfig;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Handles conversions between PDF, Word (.docx) and PowerPoint (.pptx).
 *
 * This version delegates all conversion work to LibreOffice running in
 * headless mode. LibreOffice performs real layout analysis internally
 * (text blocks, tables, images, fonts), so output preserves formatting
 * far more faithfully than a pure PDFBox/POI text-extraction approach.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final StorageConfig storageConfig;

    private static final int MAX_CONCURRENT_CONVERSIONS = 3;
    private static final long CONVERSION_TIMEOUT_SECONDS = 120;

    @Value("${libreoffice.binary-path:#{null}}")
    private String configuredLibreOfficeBinary;

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

    private final Semaphore conversionSlots = new Semaphore(MAX_CONCURRENT_CONVERSIONS, true);

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

        boolean acquired;
        try {
            acquired = conversionSlots.tryAcquire(CONVERSION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for a free conversion slot", e);
        }
        if (!acquired) {
            throw new IOException("Server busy: all conversion slots in use, please retry shortly");
        }

        Path profileDir = outputDir.resolve("lo_profile_" + uid);

        try {
            Files.createDirectories(profileDir);

            String binaryPath = resolveLibreOfficeBinary();
            log.info("Executing LibreOffice binary: {}", binaryPath);

            String filter = targetFormat.toLowerCase();
            String inputFileName = inputPath.getFileName().toString().toLowerCase();

            // Build process arguments dynamically to support PDF input filters correctly
            List<String> command = new ArrayList<>();
            command.add(binaryPath);
            command.add("--headless");
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

            String inputBaseName = stripExtension(inputPath.getFileName().toString());
            Path libreOfficeOutput = outputDir.resolve(inputBaseName + "." + targetFormat);

            if (!Files.exists(libreOfficeOutput)) {
                throw new IOException("Expected LibreOffice output not found: " + libreOfficeOutput
                        + "\nLibreOffice log:\n" + processOutput);
            }

            File outFile = outputDir.resolve(uid + "_output." + targetFormat).toFile();
            Files.move(libreOfficeOutput, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            return outFile;

        } finally {
            conversionSlots.release();
            try {
                deleteRecursive(profileDir);
            } catch (IOException e) {
                log.warn("Failed to clean up LibreOffice profile dir {}: {}", profileDir, e.getMessage());
            }
        }
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

    private void deleteRecursive(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }
}