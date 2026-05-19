package com.geeksblue.inspection.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.service.ClassroomLockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 教室占用锁端到端测试：
 * <ul>
 *   <li>未登录的角色无法访问教室列表</li>
 *   <li>A 用户占用后，B 用户看到该教室 locked=true，再次 claim 返回 409</li>
 *   <li>A 用户上传成功后，锁自动释放，B 用户随即可以占用</li>
 *   <li>持有者主动释放后，其他用户可立刻占用</li>
 *   <li>不存在的教室返回 404</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClassroomControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ClassroomLockService lockService;

    @MockBean
    AliyunVisionService visionService;

    @AfterEach
    void clearLocks() {
        // 每个用例独立持有 / 释放，确保不会相互污染
        for (String id : new String[]{"A101", "A102", "A103", "B201", "B202", "C301"}) {
            lockService.release(id, lockService.get(id) == null ? "" : lockService.get(id).userId());
        }
    }

    /** 未登录角色（缺少 X-User-Role）访问列表应被拒绝 */
    @Test
    void anonymousCannotListClassrooms() throws Exception {
        mvc.perform(get("/api/classrooms"))
                .andExpect(status().isForbidden());
    }

    /** 占用 → 看到 locked → 重复占用冲突 */
    @Test
    void claimThenAnotherUserGetsConflict() throws Exception {
        // 用户 A 占用 A101
        mvc.perform(post("/api/classrooms/A101/claim")
                        .header("X-User-Id", "stu-a")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("A101"))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.lockedBy").value("stu-a"));

        // 用户 B 看列表：A101 应该是 locked
        var res = mvc.perform(get("/api/classrooms")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = json.readTree(res.getResponse().getContentAsString());
        JsonNode a101 = null;
        for (JsonNode n : arr) {
            if ("A101".equals(n.get("id").asText())) { a101 = n; break; }
        }
        assertThat(a101).isNotNull();
        assertThat(a101.get("locked").asBoolean()).isTrue();
        assertThat(a101.get("lockedBy").asText()).isEqualTo("stu-a");

        // 用户 B 再 claim 同一间：409
        mvc.perform(post("/api/classrooms/A101/claim")
                        .header("X-User-Id", "stu-b")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isConflict());

        // 持有者 A 主动释放
        mvc.perform(post("/api/classrooms/A101/release")
                        .header("X-User-Id", "stu-a")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNoContent());

        // 释放后 B 可以占用
        mvc.perform(post("/api/classrooms/A101/claim")
                        .header("X-User-Id", "stu-b")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedBy").value("stu-b"));
    }

    /** 上传成功会释放锁，下一个用户能立刻占用 */
    @Test
    void uploadReleasesLockAutomatically() throws Exception {
        when(visionService.detect(any(Path.class))).thenReturn(new DetectionResult());

        // stu-a 占用 A102
        mvc.perform(post("/api/classrooms/A102/claim")
                        .header("X-User-Id", "stu-a")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk());

        // 上传同一间，触发自动释放
        MockMultipartFile photo = new MockMultipartFile(
                "photo", "p.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[]{1});
        mvc.perform(multipart("/api/cleaning/check")
                        .file(photo)
                        .param("classroomId", "A102")
                        .header("X-User-Id", "stu-a")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk());

        // 锁应当已释放，stu-b 立刻能占用
        mvc.perform(post("/api/classrooms/A102/claim")
                        .header("X-User-Id", "stu-b")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedBy").value("stu-b"));
    }

    /** 占用不存在的教室返回 404 */
    @Test
    void claimingUnknownClassroomReturns404() throws Exception {
        mvc.perform(post("/api/classrooms/ZZZ999/claim")
                        .header("X-User-Id", "stu-a")
                        .header("X-User-Role", "STUDENT"))
                .andExpect(status().isNotFound());
    }
}
