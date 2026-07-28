package com.pdfconverter.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversion_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String originalFileName;

    @Column(nullable = false, length = 500)
    private String convertedFileName;

    // e.g. "PDF_TO_WORD", "WORD_TO_PDF", "PDF_TO_PPT", "PPT_TO_PDF"
    @Column(nullable = false)
    private String conversionType;

    @Column(nullable = false)
    private String status; // SUCCESS or FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}