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
 * 教室照片本地存储工具。
 *
 * <p>落盘路径规则：{@code cleaning.storage-root/YYYY/MM/DD/uuid.jpg}
 */
@Component
public class PhotoStorage {

    /** 图片落盘根目录的绝对路径 */
    private final Path root;

    public PhotoStorage(@Value("${cleaning.storage-root:./data/cleaning}") String root) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
    }

    /** 获取根目录，供清理任务遍历使用 */
    public Path root() {
        return root;
    }

    /**
     * 保存上传的图片文件。
     *
     * @param file 小程序上传的图片
     * @return 落盘后的绝对路径
     */
    public Path save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传图片为空");
        }
        // 按 年/月/日 三级目录组织，便于按天清理
        LocalDate today = LocalDate.now();
        Path dir = root
                .resolve(today.format(DateTimeFormatter.ofPattern("yyyy")))
                .resolve(today.format(DateTimeFormatter.ofPattern("MM")))
                .resolve(today.format(DateTimeFormatter.ofPattern("dd")));
        Files.createDirectories(dir);

        // 使用 UUID 文件名，避免重名覆盖
        String ext = extensionOf(file.getOriginalFilename());
        Path target = dir.resolve(UUID.randomUUID() + ext);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    /**
     * 仅允许常见图片后缀，其余统一改写为 .jpg，避免上传可执行文件。
     */
    private static String extensionOf(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return ".jpg";
        String ext = filename.substring(dot).toLowerCase();
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".webp", ".heic" -> ext;
            default -> ".jpg";
        };
    }
}
