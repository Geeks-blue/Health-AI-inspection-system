package com.geeksblue.inspection.api;

import com.geeksblue.inspection.domain.CleaningRecord;

import java.time.LocalDateTime;

/**
 * 老师复核列表中的单条记录视图。
 */
public class ReviewItem {

    /** 记录 ID */
    private Long recordId;
    /** 教室编号 */
    private String classroomId;
    /** 上传人 ID */
    private String uploaderId;
    /** 图片磁盘路径 */
    private String photoPath;
    /** AI 判定原因 */
    private String aiDetail;
    /** 上传时间 */
    private LocalDateTime createdAt;

    /** 由实体转换为视图对象 */
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
