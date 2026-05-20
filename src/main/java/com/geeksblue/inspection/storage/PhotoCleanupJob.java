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
 * 图片清理定时任务。
 *
 * <p>每天 03:00 执行一次，删除超过 {@code cleaning.retention-days}（默认 7 天）的图片，
 * 并清理空目录。数据库记录不会被删除，仍可用于统计。
 */
@Component
public class PhotoCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PhotoCleanupJob.class);

    private final PhotoStorage storage;
    /** 图片保留天数 */
    private final int retentionDays;

    public PhotoCleanupJob(PhotoStorage storage,
                           @Value("${cleaning.retention-days:7}") int retentionDays) {
        this.storage = storage;
        this.retentionDays = retentionDays;
    }

    /** 每天凌晨 3 点执行 */
    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        Path root = storage.root();
        if (!Files.exists(root)) {
            return;
        }
        // 计算过期时间点
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = 0;
        try (Stream<Path> walk = Files.walk(root)) {
            // 倒序遍历：先删文件，再删空目录
            var paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                if (p.equals(root)) continue;
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    if (Files.isRegularFile(p) && attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        // 文件已过期，直接删除
                        Files.deleteIfExists(p);
                        deleted++;
                    } else if (Files.isDirectory(p) && isEmpty(p)) {
                        // 空目录顺手删掉，保持目录树整洁
                        Files.deleteIfExists(p);
                    }
                } catch (IOException e) {
                    log.warn("清理过程中处理文件失败 {}", p, e);
                }
            }
        } catch (IOException e) {
            log.error("遍历图片目录失败", e);
        }
        log.info("图片清理完成，共删除 {} 个文件（保留天数={}）", deleted, retentionDays);
    }

    /** 判断目录是否为空 */
    private static boolean isEmpty(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }
}
