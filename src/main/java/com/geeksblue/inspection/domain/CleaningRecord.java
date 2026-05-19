package com.geeksblue.inspection.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 卫生巡查记录实体。
 *
 * <p>说明：图片文件会在保留期（默认 7 天）后被定时任务清理，
 * 但本表记录会一直保留，用于后续统计报表。
 */
@Entity
@Table(name = "cleaning_record", indexes = {
        @Index(name = "idx_classroom_id", columnList = "classroom_id"),
        @Index(name = "idx_ai_result", columnList = "ai_result"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class CleaningRecord {

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 教室编号 */
    @Column(name = "classroom_id", nullable = false, length = 64)
    private String classroomId;

    /** 上传人（学生）ID */
    @Column(name = "uploader_id", nullable = false, length = 64)
    private String uploaderId;

    /** 图片在服务器上的绝对路径，可能因清理任务而失效 */
    @Column(name = "photo_path", length = 512)
    private String photoPath;

    /** AI 判定结果：pass / review */
    @Column(name = "ai_result", nullable = false, length = 16)
    private String aiResult;

    /** AI 判定原因，例如 "floor_ok, desk_ok, podium_ok, bin_ok" */
    @Column(name = "ai_detail", length = 1024)
    private String aiDetail;

    /** 复核老师 ID（仅 review 流程会写入） */
    @Column(name = "reviewer_id", length = 64)
    private String reviewerId;

    /** 最终结果：pass / fail（AI 合格时直接写 pass；待复核时由老师写入） */
    @Column(name = "final_result", length = 16)
    private String finalResult;

    /** 上传时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 复核时间 */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ===== getter / setter =====

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
