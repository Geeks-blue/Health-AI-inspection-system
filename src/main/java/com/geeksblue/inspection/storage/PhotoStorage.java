package com.geeksblue.inspection.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Persists uploaded photos to {@code cleaning.storage-root/YYYY/MM/DD/uuid.jpg}.
 */
@Component
public class PhotoStorage {

    private final Path root;

    public PhotoStorage(@Value("${cleaning.storage-root:./data/cleaning}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public Path save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("photo file is empty");
        }
        LocalDate today = LocalDate.now();
        Path dir = root
                .resolve(today.format(DateTimeFormatter.ofPattern("yyyy")))
                .resolve(today.format(DateTimeFormatter.ofPattern("MM")))
                .resolve(today.format(DateTimeFormatter.ofPattern("dd")));
        Files.createDirectories(dir);

        String ext = extensionOf(file.getOriginalFilename());
        Path target = dir.resolve(UUID.randomUUID() + ext);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static String extensionOf(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        // Accept common image extensions only; otherwise default to .jpg.
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".webp", ".heic" -> ext;
            default -> ".jpg";
        };
    }
}
