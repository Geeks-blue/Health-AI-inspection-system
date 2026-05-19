package com.geeksblue.inspection.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.domain.CleaningRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 接口集成测试，覆盖：
 * 1) AI 直接合格 → pass，无需老师介入
 * 2) AI 标记 review → 出现在待复核列表 → 老师提交 fail → 列表清空
 * 3) 角色鉴权：学生访问复核接口被拒绝
 * 4) 重复复核同一条记录 → 409 冲突
 */
@SpringBootTest
@AutoConfigureMockMvc
class CleaningControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CleaningRecordRepository repository;

    /** 用 MockBean 替换真实 AI 调用，便于精确控制返回结果 */
    @MockBean
    AliyunVisionService visionService;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    /** AI 判定干净 → 接口返回 pass，记录的 finalResult 直接写 pass */
    @Test
    void uploadPassesWhenAiDetectsClean() throws Exception {
        when(visionService.detect(any(Path.class))).thenReturn(new DetectionResult());

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "clean.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        MvcResult res = mvc.perform(multipart("/api/cleaning/check")
                        .file(photo)
                        .param("classroomId", "A101")
                        .header("X-User-Id", "stu-001")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("pass"))
                .andReturn();

        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        Long id = body.get("record_id").asLong();
        var record = repository.findById(id).orElseThrow();
        assertThat(record.getFinalResult()).isEqualTo("pass");
        assertThat(record.getReviewedAt()).isNotNull();
    }

    /** AI 报告地面垃圾 → review；老师查询列表后提交 fail；列表恢复为空 */
    @Test
    void reviewFlowEndToEnd() throws Exception {
        // 模拟 AI 在地面识别到一个 box 类型的大件垃圾
        DetectionResult d = new DetectionResult();
        d.getFloorBigTrash().add("box");
        when(visionService.detect(any(Path.class))).thenReturn(d);

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "dirty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        MvcResult uploadRes = mvc.perform(multipart("/api/cleaning/check")
                        .file(photo)
                        .param("classroomId", "B202")
                        .header("X-User-Id", "stu-002")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("review"))
                .andReturn();
        long recordId = json.readTree(uploadRes.getResponse().getContentAsString())
                .get("record_id").asLong();

        // 老师拉取待复核列表，应该有这条
        mvc.perform(get("/api/cleaning/review/list")
                        .header("X-User-Role", "TEACHER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordId").value((int) recordId))
                .andExpect(jsonPath("$[0].classroomId").value("B202"));

        // 老师提交 fail
        String submitBody = "{\"recordId\":" + recordId + ",\"result\":\"fail\"}";
        mvc.perform(post("/api/cleaning/review/submit")
                        .header("X-User-Id", "teacher-007")
                        .header("X-User-Role", "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isNoContent());

        // 复核后列表应该清空，DB 中 finalResult=fail
        mvc.perform(get("/api/cleaning/review/list")
                        .header("X-User-Role", "TEACHER"))
                .andExpect(jsonPath("$.length()").value(0));
        var saved = repository.findById(recordId).orElseThrow();
        assertThat(saved.getFinalResult()).isEqualTo("fail");
        assertThat(saved.getReviewerId()).isEqualTo("teacher-007");
    }

    /** 学生不能访问复核接口（403） */
    @Test
    void studentCannotAccessReviewList() throws Exception {
        mvc.perform(get("/api/cleaning/review/list")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isForbidden());
    }

    /** 重复复核同一条记录应返回 409 冲突 */
    @Test
    void duplicateReviewReturnsConflict() throws Exception {
        DetectionResult d = new DetectionResult();
        d.getBinOverflow().add("trashcan");
        when(visionService.detect(any(Path.class))).thenReturn(d);

        MockMultipartFile photo = new MockMultipartFile(
                "photo", "dirty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1, 2, 3});

        MvcResult res = mvc.perform(multipart("/api/cleaning/check")
                        .file(photo)
                        .param("classroomId", "C303")
                        .header("X-User-Id", "stu-003")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andReturn();
        long recordId = json.readTree(res.getResponse().getContentAsString())
                .get("record_id").asLong();

        String submitBody = "{\"recordId\":" + recordId + ",\"result\":\"pass\"}";
        mvc.perform(post("/api/cleaning/review/submit")
                        .header("X-User-Role", "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isNoContent());
        // 第二次提交应被拒绝
        mvc.perform(post("/api/cleaning/review/submit")
                        .header("X-User-Role", "TEACHER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isConflict());
    }
}
