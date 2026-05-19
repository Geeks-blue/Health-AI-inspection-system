package com.geeksblue.inspection.service;

import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.CleaningRuleEngine;
import com.geeksblue.inspection.ai.CleaningRuleEngine.Judgement;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.api.CheckResponse;
import com.geeksblue.inspection.api.ReviewItem;
import com.geeksblue.inspection.domain.CleaningRecord;
import com.geeksblue.inspection.domain.CleaningRecordRepository;
import com.geeksblue.inspection.storage.PhotoStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CleaningService {

    private final PhotoStorage photoStorage;
    private final AliyunVisionService visionService;
    private final CleaningRuleEngine ruleEngine;
    private final CleaningRecordRepository repository;

    public CleaningService(PhotoStorage photoStorage,
                           AliyunVisionService visionService,
                           CleaningRuleEngine ruleEngine,
                           CleaningRecordRepository repository) {
        this.photoStorage = photoStorage;
        this.visionService = visionService;
        this.ruleEngine = ruleEngine;
        this.repository = repository;
    }

    @Transactional
    public CheckResponse check(MultipartFile photo, String classroomId, String uploaderId) throws IOException {
        Path saved = photoStorage.save(photo);
        DetectionResult detection = visionService.detect(saved);
        Judgement judgement = ruleEngine.judge(detection);

        CleaningRecord record = new CleaningRecord();
        record.setClassroomId(classroomId);
        record.setUploaderId(uploaderId);
        record.setPhotoPath(saved.toString());
        record.setAiResult(judgement.result());
        record.setAiDetail(judgement.reason());
        record.setCreatedAt(LocalDateTime.now());
        // If AI passes, the inspection is final without teacher review.
        if (CleaningRuleEngine.PASS.equals(judgement.result())) {
            record.setFinalResult("pass");
            record.setReviewedAt(record.getCreatedAt());
        }
        record = repository.save(record);

        return new CheckResponse(judgement.result(), judgement.reason(), String.valueOf(record.getId()));
    }

    public List<ReviewItem> pendingReviews() {
        return repository.findByAiResultAndFinalResultIsNullOrderByCreatedAtAsc(CleaningRuleEngine.REVIEW)
                .stream().map(ReviewItem::from).toList();
    }

    @Transactional
    public void submitReview(Long recordId, String reviewerId, String result) {
        if (!"pass".equals(result) && !"fail".equals(result)) {
            throw new IllegalArgumentException("result must be 'pass' or 'fail'");
        }
        CleaningRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("record not found: " + recordId));
        if (record.getFinalResult() != null) {
            throw new IllegalStateException("record already reviewed: " + recordId);
        }
        record.setReviewerId(reviewerId);
        record.setFinalResult(result);
        record.setReviewedAt(LocalDateTime.now());
        repository.save(record);
    }
}
