package com.geeksblue.inspection.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Persistent record of a classroom cleaning inspection.
 * Photo files are removed after retention-days, but the row is kept for stats.
 */
@Entity
@Table(name = "cleaning_record", indexes = {
        @Index(name = "idx_classroom_id", columnList = "classroom_id"),
        @Index(name = "idx_ai_result", columnList = "ai_result"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class CleaningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false, length = 64)
    private String classroomId;

    @Column(name = "uploader_id", nullable = false, length = 64)
    private String uploaderId;

    @Column(name = "photo_path", length = 512)
    private String photoPath;

    /** "pass" or "review" — produced by the AI rule engine. */
    @Column(name = "ai_result", nullable = false, length = 16)
    private String aiResult;

    /** Human-readable reasons, e.g. "floor_ok, desk_ok, podium_ok, bin_ok". */
    @Column(name = "ai_detail", length = 1024)
    private String aiDetail;

    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    /** "pass" or "fail" — set only after teacher review. */
    @Column(name = "final_result", length = 16)
    private String finalResult;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClassroomId() { return classroomId; }
    public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

    public String getUploaderId() { return uploaderId; }
    public void setUploaderId(String uploaderId) { this.uploaderId = uploaderId; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getAiResult() { return aiResult; }
    public void setAiResult(String aiResult) { this.aiResult = aiResult; }

    public String getAiDetail() { return aiDetail; }
    public void setAiDetail(String aiDetail) { this.aiDetail = aiDetail; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getFinalResult() { return finalResult; }
    public void setFinalResult(String finalResult) { this.finalResult = finalResult; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}
