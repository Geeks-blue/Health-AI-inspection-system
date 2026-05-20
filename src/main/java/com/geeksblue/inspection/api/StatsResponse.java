package com.geeksblue.inspection.api;

import java.util.List;

/**
 * 管理员统计接口的返回体。
 */
public class StatsResponse {

    /** 总记录数 */
    private long totalRecords;
    /** AI 直接合格的次数 */
    private long aiPassCount;
    /** AI 标记为待复核的次数 */
    private long aiReviewCount;
    /** 老师最终判定为不合格的次数 */
    private long finalFailCount;
    /** 待复核（仍未处理）的次数 */
    private long pendingReviewCount;
    /** 按教室拆分的明细 */
    private List<ClassroomStat> classrooms;

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getAiPassCount() { return aiPassCount; }
    public void setAiPassCount(long aiPassCount) { this.aiPassCount = aiPassCount; }

    public long getAiReviewCount() { return aiReviewCount; }
    public void setAiReviewCount(long aiReviewCount) { this.aiReviewCount = aiReviewCount; }

    public long getFinalFailCount() { return finalFailCount; }
    public void setFinalFailCount(long finalFailCount) { this.finalFailCount = finalFailCount; }

    public long getPendingReviewCount() { return pendingReviewCount; }
    public void setPendingReviewCount(long pendingReviewCount) { this.pendingReviewCount = pendingReviewCount; }

    public List<ClassroomStat> getClassrooms() { return classrooms; }
    public void setClassrooms(List<ClassroomStat> classrooms) { this.classrooms = classrooms; }

    /** 单个教室的统计明细 */
    public static class ClassroomStat {
        private String classroomId;
        private long total;
        private long aiPass;
        private long aiReview;
        private long finalFail;

        public ClassroomStat() {}

        public ClassroomStat(String classroomId, long total, long aiPass, long aiReview, long finalFail) {
            this.classroomId = classroomId;
            this.total = total;
            this.aiPass = aiPass;
            this.aiReview = aiReview;
            this.finalFail = finalFail;
        }

        public String getClassroomId() { return classroomId; }
        public void setClassroomId(String classroomId) { this.classroomId = classroomId; }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public long getAiPass() { return aiPass; }
        public void setAiPass(long aiPass) { this.aiPass = aiPass; }

        public long getAiReview() { return aiReview; }
        public void setAiReview(long aiReview) { this.aiReview = aiReview; }

        public long getFinalFail() { return finalFail; }
        public void setFinalFail(long finalFail) { this.finalFail = finalFail; }
    }
}
