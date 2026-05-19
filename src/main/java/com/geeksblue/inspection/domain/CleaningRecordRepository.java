package com.geeksblue.inspection.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 卫生巡查记录仓库。
 */
public interface CleaningRecordRepository extends JpaRepository<CleaningRecord, Long> {

    /**
     * 查询所有「AI 标记为 review 且尚未老师复核」的记录，按上传时间升序。
     * 用于老师的待复核列表。
     */
    List<CleaningRecord> findByAiResultAndFinalResultIsNullOrderByCreatedAtAsc(String aiResult);

    /**
     * 按教室聚合统计：总次数、AI 直接合格次数、待复核次数、最终不合格次数。
     * 返回 Object[]：[classroomId, total, aiPass, aiReview, finalFail]
     */
    @Query("""
        SELECT r.classroomId,
               COUNT(r),
               SUM(CASE WHEN r.aiResult = 'pass' THEN 1 ELSE 0 END),
               SUM(CASE WHEN r.aiResult = 'review' THEN 1 ELSE 0 END),
               SUM(CASE WHEN r.finalResult = 'fail' THEN 1 ELSE 0 END)
        FROM CleaningRecord r
        WHERE (:from IS NULL OR r.createdAt >= :from)
          AND (:to IS NULL OR r.createdAt < :to)
        GROUP BY r.classroomId
        ORDER BY r.classroomId
        """)
    List<Object[]> aggregateByClassroom(LocalDateTime from, LocalDateTime to);
}
