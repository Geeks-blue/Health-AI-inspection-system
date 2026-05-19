package com.geeksblue.inspection.domain;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
