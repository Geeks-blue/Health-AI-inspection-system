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

@RestController
@RequestMapping("/api/cleaning")
public class CleaningController {

    private final CleaningService service;

    public CleaningController(CleaningService service) {
        this.service = service;
    }

    /** Student uploads a classroom photo for automated AI judgement. */
    @PostMapping(path = "/check", consumes = "multipart/form-data")
    public CheckResponse check(@RequestPart("photo") MultipartFile photo,
                               @RequestParam("classroomId") String classroomId,
                               @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String uploaderId,
                               @RequestHeader(value = "X-User-Role", required = false) String roleHeader)
            throws IOException {
        Role role = Role.parse(roleHeader);
        if (role != Role.STUDENT && role != Role.TEACHER && role != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "upload requires an authenticated user");
        }
        return service.check(photo, classroomId, uploaderId);
    }

    /** Teacher fetches the list of records flagged by AI as needing review. */
    @GetMapping("/review/list")
    public List<ReviewItem> reviewList(
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireTeacherOrAdmin(roleHeader);
        return service.pendingReviews();
    }

    /** Teacher confirms or overturns the AI judgement. */
    @PostMapping("/review/submit")
    public ResponseEntity<Void> reviewSubmit(
            @Valid @RequestBody ReviewSubmitRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "teacher") String reviewerId,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        requireTeacherOrAdmin(roleHeader);
        service.submitReview(request.getRecordId(), reviewerId, request.getResult());
        return ResponseEntity.noContent().build();
    }

    private void requireTeacherOrAdmin(String roleHeader) {
        Role role = Role.parse(roleHeader);
        if (role != Role.TEACHER && role != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "teacher role required");
        }
    }
}
