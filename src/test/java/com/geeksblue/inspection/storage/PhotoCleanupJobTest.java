package com.geeksblue.inspection.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 7 天清理任务单元测试：
 * <ul>
 *   <li>修改时间在保留期之外的旧文件 → 被删除</li>
 *   <li>修改时间在保留期之内的新文件 → 保留</li>
 *   <li>空目录 → 一起清理</li>
 * </ul>
 */
class PhotoCleanupJobTest {

    @Test
    void deletesFilesOlderThanRetention(@TempDir Path tmp) throws IOException {
        // 准备一个旧文件（10 天前）和一个新文件（1 小时前）
        Path oldDir = Files.createDirectories(tmp.resolve("2026/05/05"));
        Path newDir = Files.createDirectories(tmp.resolve("2026/05/19"));
        Path oldFile = Files.write(oldDir.resolve("old.jpg"), new byte[]{1});
        Path newFile = Files.write(newDir.resolve("new.jpg"), new byte[]{2});
        Files.setLastModifiedTime(oldFile,
                FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS)));
        Files.setLastModifiedTime(newFile,
                FileTime.from(Instant.now().minus(1, ChronoUnit.HOURS)));

        // PhotoStorage 只用到 root()，用 mock 代替真实实例
        PhotoStorage storage = mock(PhotoStorage.class);
        when(storage.root()).thenReturn(tmp);

        new PhotoCleanupJob(storage, 7).run();

        assertThat(Files.exists(oldFile)).as("过期文件应被删除").isFalse();
        assertThat(Files.exists(newFile)).as("新文件应保留").isTrue();
        assertThat(Files.exists(oldDir)).as("空目录也应被清理").isFalse();
    }
}
