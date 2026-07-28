package com.pdfconverter.controller;

import com.pdfconverter.model.ConversionHistory;
import com.pdfconverter.repository.ConversionHistoryRepository;
import com.pdfconverter.service.ConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/convert")
@RequiredArgsConstructor
public class ConversionController {

    private final ConversionService conversionService;
    private final ConversionHistoryRepository historyRepository;

    /**
     * Single, simple endpoint for every conversion direction.
     * type must be one of: PDF_TO_WORD, WORD_TO_PDF, PDF_TO_PPT, PPT_TO_PDF
     */
    @PostMapping
    public ResponseEntity<?> convert(@RequestParam("file") MultipartFile file,
                                      @RequestParam("type") String type) {
        ConversionService.ConversionType conversionType;
        try {
            conversionType = ConversionService.ConversionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown conversion type. Use one of: PDF_TO_WORD, WORD_TO_PDF, PDF_TO_PPT, PPT_TO_PDF"));
        }

        try {
            ConversionService.ConversionResult result = conversionService.convert(file, conversionType);

            historyRepository.save(new ConversionHistory(
                    null, file.getOriginalFilename(), result.fileName(), conversionType.name(), "SUCCESS", null,
                    java.time.LocalDateTime.now()));

            FileSystemResource resource = new FileSystemResource(result.file());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            historyRepository.save(new ConversionHistory(
                    null, file.getOriginalFilename(), "-", conversionType.name(), "FAILED", e.getMessage(),
                    java.time.LocalDateTime.now()));
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Conversion failed: " + e.getMessage()));
        }
    }

    @GetMapping("/history")
    public List<ConversionHistory> getHistory() {
        return historyRepository.findAllByOrderByCreatedAtDesc();
    }
}
