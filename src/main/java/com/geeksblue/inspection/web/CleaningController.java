package com.geeksblue.inspection.web;

import com.geeksblue.inspection.api.CheckResponse;
import com.geeksblue.inspection.api.ReviewItem;
import com.geeksblue.inspection.api.ReviewSubmitRequest;
import com.geeksblue.inspection.security.Role;
import com.geeksblue.inspection.service.CleaningService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * 卫生巡查 REST 控制器。
 *
 * <p>请求头约定：
 * <ul>
 *   <li>{@code X-User-Id}：用户 ID</li>
 *   <li>{@code X-User-Role}：STUDENT / TEACHER / ADMIN</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cleaning")
public class CleaningController {

    private final CleaningService service;

    public CleaningController(CleaningService service) {
        this.service = service;
    }

    /** 学生：上传教室照片，AI 自动判定 */
    @PostMapping(path = "/check", consumes = "multipart/form-data")
    public CheckResponse check(@RequestPart("photo") MultipartFile photo,
                               @RequestParam("classroomId") String classroomId,
                               @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String uploaderId,
                               @RequestHeader(value = "X-User-Role", required = false) String roleHeader)
            throws IOException {
        // 任意已登录角色都可上传，但匿名/未识别角色被拒绝
        Role role = Role.parse(roleHeader);
        if (role != Role.STUDENT && role != Role.TEACHER && role != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "上传需要已登录的用户");
        }
        return service.check(photo, classroomId, uploaderId);
    }

    /** 老师：拉取待复核的记录列表 */
    @GetMapping("/review/list")
    public List<ReviewItem> reviewList(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireTeacherOrAdmin(roleHeader);
        return service.pendingReviews();
    }

    /** 老师：提交复核结果（pass / fail） */
    @PostMapping("/review/submit")
    public ResponseEntity<Void> reviewSubmit(
            @Valid @RequestBody ReviewSubmitRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "teacher") String reviewerId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireTeacherOrAdmin(roleHeader);
        service.submitReview(request.getRecordId(), reviewerId, request.getResult());
        return ResponseEntity.noContent().build();
    }

    /** 仅老师 / 管理员可访问复核接口 */
    private void requireTeacherOrAdmin(String roleHeader) {
        Role role = Role.parse(roleHeader);
        if (role != Role.TEACHER && role != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "需要老师权限");
        }
    }
}
