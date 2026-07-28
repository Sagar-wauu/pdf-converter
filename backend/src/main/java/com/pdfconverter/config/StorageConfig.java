package com.pdfconverter.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Getter
public class StorageConfig {

    @Value("${app.storage.temp-dir}")
    private String tempDir;

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(tempDir));
        Files.createDirectories(Paths.get(uploadDir));
    }

    public Path tempPath() {
        return Paths.get(tempDir);
    }

    public Path uploadPath() {
        return Paths.get(uploadDir);
    }
}
