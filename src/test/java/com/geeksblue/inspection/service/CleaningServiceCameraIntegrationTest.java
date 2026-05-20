package com.geeksblue.inspection.service;

import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.camera.CameraSnapshotService;
import com.geeksblue.inspection.domain.CleaningRecord;
import com.geeksblue.inspection.domain.CleaningRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 「学生上传同时同步抓拍监控」流程的集成测试：
 *
 * <ul>
 *   <li>学生上传干净 + 监控也干净 → pass</li>
 *   <li>学生看着干净，但监控发现地面垃圾 → review（监控结果会合并）</li>
 *   <li>监控抓帧失败时不阻塞流程，aiDetail 末尾追加 camera_unavailable</li>
 *   <li>记录里 cameraPhotoPath 在抓拍成功时被写入</li>
 * </ul>
 */
@SpringBootTest
class CleaningServiceCameraIntegrationTest {

    @Autowired CleaningService service;
    @Autowired CleaningRecordRepository repository;

    @MockBean AliyunVisionService visionService;
    @MockBean CameraSnapshotService cameraService;

    @BeforeEach
    void clean() { repository.deleteAll(); }

    @AfterEach
    void clearMocks() { /* MockBean 自动重置 */ }

    /** 两路 AI 都干净 → pass */
    @Test
    void passWhenBothViewsClean() throws Exception {
        when(cameraService.snapshot(anyString(), any(Path.class))).thenAnswer(inv -> {
            Files.createDirectories(((Path) inv.getArgument(1)).getParent());
            Files.write((Path) inv.getArgument(1), new byte[]{1});
            return true;
        });
        when(visionService.detect(any(Path.class))).thenReturn(new DetectionResult());

        var resp = service.check(
                new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[]{1}),
                "A101", "stu-1");
        assertThat(resp.getResult()).isEqualTo("pass");
        // 学生 + 监控两次 detect
        verify(visionService, atLeastOnce()).detect(any(Path.class));

        CleaningRecord saved = repository.findById(Long.valueOf(resp.getRecordId())).orElseThrow();
        assertThat(saved.getCameraPhotoPath()).isNotNull();
        assertThat(saved.getAiResult()).isEqualTo("pass");
        assertThat(saved.getAiDetail()).doesNotContain("camera_unavailable");
    }

    /** 学生看着干净，监控发现地面垃圾 → 整体 review，原因里包含 floor_big_trash */
    @Test
    void cameraDetectsTrashOverridesPass() throws Exception {
        when(cameraService.snapshot(anyString(), any(Path.class))).thenAnswer(inv -> {
            Files.createDirectories(((Path) inv.getArgument(1)).getParent());
            Files.write((Path) inv.getArgument(1), new byte[]{1});
            return true;
        });
        // 用学生图 / 监控图分别返回不同的检测结果
        ArgumentCaptor<Path> pathCaptor = ArgumentCaptor.forClass(Path.class);
        DetectionResult cleanResult = new DetectionResult();
        DetectionResult trashResult = new DetectionResult();
        trashResult.getFloorBigTrash().add("box");
        // 第一次调用（学生图）返回干净，第二次（监控图）返回脏
        when(visionService.detect(pathCaptor.capture()))
                .thenReturn(cleanResult)
                .thenReturn(trashResult);

        var resp = service.check(
                new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[]{1}),
                "A101", "stu-1");
        assertThat(resp.getResult()).isEqualTo("review");
        assertThat(resp.getReason()).contains("floor_big_trash");

        CleaningRecord saved = repository.findById(Long.valueOf(resp.getRecordId())).orElseThrow();
        assertThat(saved.getCameraPhotoPath()).isNotNull();
        assertThat(saved.getFinalResult()).isNull(); // 等老师复核
    }

    /** 该教室有 cameraUrl 但抓帧失败 → 流程不阻塞，aiDetail 末尾追加 camera_unavailable */
    @Test
    void cameraFailureDoesNotBlockUpload() throws Exception {
        // 摄像头有 URL 但抓帧失败
        when(cameraService.snapshot(anyString(), any(Path.class))).thenReturn(false);
        when(visionService.detect(any(Path.class))).thenReturn(new DetectionResult());

        var resp = service.check(
                new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[]{1}),
                "A101", "stu-1");
        assertThat(resp.getResult()).isEqualTo("pass");
        assertThat(resp.getReason()).endsWith("camera_unavailable");

        CleaningRecord saved = repository.findById(Long.valueOf(resp.getRecordId())).orElseThrow();
        assertThat(saved.getCameraPhotoPath()).isNull();
        assertThat(saved.getAiDetail()).contains("camera_unavailable");
    }

    /** 教室无 cameraUrl（A103）→ 不去抓监控，也不追加 camera_unavailable */
    @Test
    void classroomWithoutCameraUrlSkipsSilently() throws Exception {
        when(visionService.detect(any(Path.class))).thenReturn(new DetectionResult());

        var resp = service.check(
                new MockMultipartFile("photo", "p.jpg", "image/jpeg", new byte[]{1}),
                "A103", "stu-1");
        assertThat(resp.getResult()).isEqualTo("pass");
        assertThat(resp.getReason()).doesNotContain("camera_unavailable");

        CleaningRecord saved = repository.findById(Long.valueOf(resp.getRecordId())).orElseThrow();
        assertThat(saved.getCameraPhotoPath()).isNull();
    }
}
