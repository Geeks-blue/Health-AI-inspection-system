package com.geeksblue.inspection.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleaningRecordRepository extends JpaRepository<CleaningRecord, Long> {

    List<CleaningRecord> findByAiResultAndFinalResultIsNullOrderByCreatedAtAsc(String aiResult);
}
