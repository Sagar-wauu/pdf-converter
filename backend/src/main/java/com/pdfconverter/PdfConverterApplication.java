package com.pdfconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the PDF Converter application.
 *
 * This backend exposes two main feature areas:
 *  1. /api/convert   - convert files between PDF, Word (.docx) and PowerPoint (.pptx)
 *  2. /api/coverpage  - generate a customizable university-style cover (front) page as a PDF
 */
@SpringBootApplication
@EnableScheduling // used to periodically clean up temporary converted files
public class PdfConverterApplication {
    public static void main(String[] args) {
        SpringApplication.run(PdfConverterApplication.class, args);
    }
}
