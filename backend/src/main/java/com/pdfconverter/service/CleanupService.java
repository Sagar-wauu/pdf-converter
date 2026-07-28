package com.pdfconverter.service;

import com.pdfconverter.config.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;

/** Deletes uploaded/converted files older than 1 hour to keep the server tidy. */
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final StorageConfig storageConfig;
    private static final long MAX_AGE_MILLIS = 60L * 60 * 1000; // 1 hour

    @Scheduled(fixedRate = 30 * 60 * 1000) // every 30 minutes
    public void cleanup() {
        cleanDirectory(storageConfig.tempPath());
        cleanDirectory(storageConfig.uploadPath());
    }

    private void cleanDirectory(Path dir) {
        File folder = dir.toFile();
        File[] files = folder.listFiles();
        if (files == null) return;

        long now = Instant.now().toEpochMilli();
        for (File f : files) {
            if (f.isFile() && (now - f.lastModified()) > MAX_AGE_MILLIS) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
    }
}
