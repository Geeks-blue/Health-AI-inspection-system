package com.geeksblue.inspection.web;

import com.geeksblue.inspection.api.ClassroomDto;
import com.geeksblue.inspection.domain.Classroom;
import com.geeksblue.inspection.domain.ClassroomRepository;
import com.geeksblue.inspection.security.Role;
import com.geeksblue.inspection.service.ClassroomLockService;
import com.geeksblue.inspection.service.ClassroomLockService.ClaimOutcome;
import com.geeksblue.inspection.service.ClassroomLockService.Lock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 教室相关接口：列表、占用、释放。
 *
 * <p>列表对所有已登录用户开放（前端要拿来渲染选择框）；
 * 占用 / 释放需要 STUDENT 及以上身份。
 */
@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {

    private final ClassroomRepository repository;
    private final ClassroomLockService lockService;

    public ClassroomController(ClassroomRepository repository, ClassroomLockService lockService) {
        this.repository = repository;
        this.lockService = lockService;
    }

    /** 列出所有教室及其当前占用状态 */
    @GetMapping
    public List<ClassroomDto> list(@RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireAnyUser(roleHeader);
        return repository.findAll().stream()
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(this::toDto)
                .toList();
    }

    /** 占用某间教室；冲突时返回 409 */
    @PostMapping("/{id}/claim")
    public ClassroomDto claim(@PathVariable("id") String id,
                              @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
                              @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireAnyUser(roleHeader);
        // 教室必须存在，避免任意字符串都能创建锁
        Classroom c = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "教室不存在: " + id));
        ClaimOutcome outcome = lockService.claim(id, userId);
        if (!outcome.ok()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "教室已被 " + outcome.current().userId() + " 占用");
        }
        return toDto(c);
    }

    /** 释放某间教室；非持有者调用会被忽略，统一 204 */
    @PostMapping("/{id}/release")
    public ResponseEntity<Void> release(@PathVariable("id") String id,
                                        @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
                                        @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireAnyUser(roleHeader);
        lockService.release(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** 任意学生 / 老师 / 管理员均可，匿名拒绝 */
    private void requireAnyUser(String roleHeader) {
        Role role = Role.parse(roleHeader);
        if (roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "需要登录");
        }
        if (role != Role.STUDENT && role != Role.TEACHER && role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "角色无效");
        }
    }

    /** Classroom 实体转 DTO，带最新锁状态 */
    private ClassroomDto toDto(Classroom c) {
        Lock l = lockService.get(c.getId());
        if (l == null) {
            return new ClassroomDto(c.getId(), c.getName(), false, null, null);
        }
        return new ClassroomDto(c.getId(), c.getName(), true,
                l.userId(), l.expiresAt().toString());
    }
}
