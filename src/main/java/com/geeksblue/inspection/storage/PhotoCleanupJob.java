package com.geeksblue.inspection.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Removes photos older than {@code cleaning.retention-days} (default 7) every
 * day at 03:00 server time. Database records are intentionally preserved so
 * statistics remain accurate.
 */
@Component
public class PhotoCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PhotoCleanupJob.class);

    private final PhotoStorage storage;
    private final int retentionDays;

    public PhotoCleanupJob(PhotoStorage storage,
                           @Value("${cleaning.retention-days:7}") int retentionDays) {
        this.storage = storage;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        Path root = storage.root();
        if (!Files.exists(root)) {
            return;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            // Walk depth-first so empty directories get pruned after their files.
            var paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                if (p.equals(root)) continue;
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    if (Files.isRegularFile(p) && attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(p);
                        deleted++;
                    } else if (Files.isDirectory(p) && isEmpty(p)) {
                        Files.deleteIfExists(p);
                    }
                } catch (IOException e) {
                    log.warn("Failed to inspect/delete {}", p, e);
                }
            }
        } catch (IOException e) {
            log.error("Photo cleanup walk failed", e);
        }
        log.info("Photo cleanup removed {} files older than {} days", deleted, retentionDays);
    }

    private static boolean isEmpty(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }
}
