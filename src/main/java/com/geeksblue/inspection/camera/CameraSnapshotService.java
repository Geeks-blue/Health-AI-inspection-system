package com.geeksblue.inspection.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 教室监控摄像头抓拍服务。
 *
 * <p>当前实现：直接 HTTP GET 摄像头/NVR 的 snapshot 端点，把返回的 JPEG 写到本地磁盘。
 * 适用于绝大多数 IP 摄像头（海康 ISAPI、大华 cgi-bin/snapshot 等都提供这种端点）。
 *
 * <p>配置项 {@code cleaning.camera-mock=true}（默认）时，本服务不真正发请求，
 * 直接落一张 1 字节的占位 JPEG，方便沙箱 / 单元测试不依赖真摄像头。
 */
@Service
public class CameraSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(CameraSnapshotService.class);

    private final boolean mock;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public CameraSnapshotService(
            @Value("${cleaning.camera-mock:true}") boolean mock,
            @Value("${cleaning.camera-connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${cleaning.camera-read-timeout-ms:5000}") long readTimeoutMs) {
        this.mock = mock;
        this.connectTimeout = Duration.ofMillis(connectTimeoutMs);
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
    }

    /**
     * 从 cameraUrl 拉取一帧画面，写入到 targetPath。
     *
     * @param cameraUrl 摄像头快照端点；null/空表示教室没有摄像头
     * @param targetPath 写入位置
     * @return true 表示拉取成功；false 表示跳过（无 URL）或失败（不抛异常，由上层决定如何处理）
     */
    public boolean snapshot(String cameraUrl, Path targetPath) {
        if (cameraUrl == null || cameraUrl.isBlank()) {
            return false;
        }
        try {
            Files.createDirectories(targetPath.getParent());
        } catch (IOException e) {
            log.warn("创建监控快照目录失败 {}", targetPath, e);
            return false;
        }

        // mock 模式：写一张占位 JPEG（JPEG SOI 头 + EOI 尾），AI 仍然会被调用，规则照常工作
        if (mock) {
            try {
                Files.write(targetPath, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9});
                log.info("camera-mock 模式：写入占位监控快照 {}", targetPath);
                return true;
            } catch (IOException e) {
                log.warn("写入占位快照失败 {}", targetPath, e);
                return false;
            }
        }

        // 真实模式：HTTP GET 摄像头 snapshot 端点
        HttpURLConnection conn = null;
        try {
            URL url = URI.create(cameraUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout((int) connectTimeout.toMillis());
            conn.setReadTimeout((int) readTimeout.toMillis());
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                log.warn("摄像头 {} 返回 HTTP {}，跳过监控比对", cameraUrl, code);
                return false;
            }
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("成功抓拍监控快照：{} → {}", cameraUrl, targetPath);
            return true;
        } catch (IOException e) {
            log.warn("从摄像头 {} 抓帧失败：{}", cameraUrl, e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
