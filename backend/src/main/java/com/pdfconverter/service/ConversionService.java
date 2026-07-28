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
import java.util.Comparator;
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

            // Dynamically select the correct export filter based on the target format
            String filter = switch (targetFormat.toLowerCase()) {
                case "pdf" -> "pdf";
                case "docx" -> "docx:Office Open XML Text";
                case "pptx" -> "impress_pdf_Export";
                default -> targetFormat;
            };

            if ("pptx".equals(targetFormat.toLowerCase())) {
                filter = "Impress MS PowerPoint";
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    binaryPath, 
                    "--headless", 
                    "-env:UserInstallation=" + profileDir.toUri(),
                    "--convert-to", 
                    filter, 
                    "--outdir", 
                    outputDir.toString(), 
                    inputPath.toString()
            );
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

    private void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        if (!p.toFile().delete()) {
                            log.debug("Could not delete {}", p);
                        }
                    });
        }
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot == -1 ? fileName : fileName.substring(0, dot);
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "file";
        }
        String fileName = Path.of(originalName).getFileName().toString();
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}