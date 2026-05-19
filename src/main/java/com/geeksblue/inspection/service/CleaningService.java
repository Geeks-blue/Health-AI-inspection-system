package com.geeksblue.inspection.service;

import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.CleaningRuleEngine;
import com.geeksblue.inspection.ai.CleaningRuleEngine.Judgement;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.api.CheckResponse;
import com.geeksblue.inspection.api.ReviewItem;
import com.geeksblue.inspection.api.StatsResponse;
import com.geeksblue.inspection.api.StatsResponse.ClassroomStat;
import com.geeksblue.inspection.domain.CleaningRecord;
import com.geeksblue.inspection.domain.CleaningRecordRepository;
import com.geeksblue.inspection.storage.PhotoStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 卫生巡查业务编排服务。
 *
 * <p>负责串联：图片落盘 → 调用 AI → 规则判定 → 写入数据库 → 老师复核流程。
 */
@Service
public class CleaningService {

    private final PhotoStorage photoStorage;
    private final AliyunVisionService visionService;
    private final CleaningRuleEngine ruleEngine;
    private final CleaningRecordRepository repository;
    private final ClassroomLockService lockService;

    public CleaningService(PhotoStorage photoStorage,
                           AliyunVisionService visionService,
                           CleaningRuleEngine ruleEngine,
                           CleaningRecordRepository repository,
                           ClassroomLockService lockService) {
        this.photoStorage = photoStorage;
        this.visionService = visionService;
        this.ruleEngine = ruleEngine;
        this.repository = repository;
        this.lockService = lockService;
    }

    /**
     * 学生上传图片后的核心入口：落盘 + AI + 规则 + 存库。
     */
    @Transactional
    public CheckResponse check(MultipartFile photo, String classroomId, String uploaderId) throws IOException {
        // 1) 图片落盘
        Path saved = photoStorage.save(photo);
        // 2) 调用阿里云 AI 做目标检测
        DetectionResult detection = visionService.detect(saved);
        // 3) 规则引擎给出 pass / review
        Judgement judgement = ruleEngine.judge(detection);

        // 4) 写入数据库
        CleaningRecord record = new CleaningRecord();
        record.setClassroomId(classroomId);
        record.setUploaderId(uploaderId);
        record.setPhotoPath(saved.toString());
        record.setAiResult(judgement.result());
        record.setAiDetail(judgement.reason());
        record.setCreatedAt(LocalDateTime.now());
        // AI 直接合格的，最终结果直接写 pass，不需要老师介入
        if (CleaningRuleEngine.PASS.equals(judgement.result())) {
            record.setFinalResult("pass");
            record.setReviewedAt(record.getCreatedAt());
        }
        record = repository.save(record);

        // 6) 上传成功，立即释放该教室的占用锁（如果是自己持有），让别人可以接着选
        lockService.release(classroomId, uploaderId);

        // 7) 返回给小程序
        return new CheckResponse(judgement.result(), judgement.reason(), String.valueOf(record.getId()));
    }

    /** 老师查看待复核列表 */
    public List<ReviewItem> pendingReviews() {
        return repository.findByAiResultAndFinalResultIsNullOrderByCreatedAtAsc(CleaningRuleEngine.REVIEW)
                .stream().map(ReviewItem::from).toList();
    }

    /**
     * 老师提交复核结果。
     *
     * @param recordId   记录 ID
     * @param reviewerId 复核老师 ID（来自请求头）
     * @param result     复核结论：pass / fail
     */
    @Transactional
    public void submitReview(Long recordId, String reviewerId, String result) {
        // 参数校验：只接受 pass / fail
        if (!"pass".equals(result) && !"fail".equals(result)) {
            throw new IllegalArgumentException("复核结果必须是 pass 或 fail");
        }
        CleaningRecord record = repository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在: " + recordId));
        // 防止重复复核
        if (record.getFinalResult() != null) {
            throw new IllegalStateException("该记录已复核: " + recordId);
        }
        record.setReviewerId(reviewerId);
        record.setFinalResult(result);
        record.setReviewedAt(LocalDateTime.now());
        repository.save(record);
    }

    /**
     * 管理员统计接口。
     *
     * @param from 起始时间（含），null 表示不限
     * @param to   结束时间（不含），null 表示不限
     */
    public StatsResponse stats(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = repository.aggregateByClassroom(from, to);
        List<ClassroomStat> classrooms = new ArrayList<>(rows.size());
        long total = 0, aiPass = 0, aiReview = 0, finalFail = 0;
        for (Object[] r : rows) {
            // 不同 DB 对 SUM/COUNT 的返回类型不一致，统一走 Number 接口转 long
            String classroomId = (String) r[0];
            long t = ((Number) r[1]).longValue();
            long p = r[2] == null ? 0 : ((Number) r[2]).longValue();
            long rv = r[3] == null ? 0 : ((Number) r[3]).longValue();
            long ff = r[4] == null ? 0 : ((Number) r[4]).longValue();
            classrooms.add(new ClassroomStat(classroomId, t, p, rv, ff));
            total += t; aiPass += p; aiReview += rv; finalFail += ff;
        }
        long pending = repository
                .findByAiResultAndFinalResultIsNullOrderByCreatedAtAsc(CleaningRuleEngine.REVIEW)
                .size();

        StatsResponse resp = new StatsResponse();
        resp.setTotalRecords(total);
        resp.setAiPassCount(aiPass);
        resp.setAiReviewCount(aiReview);
        resp.setFinalFailCount(finalFail);
        resp.setPendingReviewCount(pending);
        resp.setClassrooms(classrooms);
        return resp;
    }
}
