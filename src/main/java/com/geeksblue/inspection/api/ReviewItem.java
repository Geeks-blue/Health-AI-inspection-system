package com.geeksblue.inspection.api;

import com.geeksblue.inspection.domain.CleaningRecord;

import java.time.LocalDateTime;

/**
 * Summary view of a record returned to teachers for review.
 */
public class ReviewItem {

    private Long recordId;
    private String classroomId;
    private String uploaderId;
    private String photoPath;
    private String aiDetail;
    private LocalDateTime createdAt;

    public static ReviewItem from(CleaningRecord r) {
        ReviewItem item = new ReviewItem();
        item.recordId = r.getId();
        item.classroomId = r.getClassroomId();
        item.uploaderId = r.getUploaderId();
        item.photoPath = r.getPhotoPath();
        item.aiDetail = r.getAiDetail();
        item.createdAt = r.getCreatedAt();
        return item;
    }

    public Long getRecordId() { return recordId; }
    public String getClassroomId() { return classroomId; }
    public String getUploaderId() { return uploaderId; }
    public String getPhotoPath() { return photoPath; }
    public String getAiDetail() { return aiDetail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
