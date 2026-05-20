package com.geeksblue.inspection.service;

import com.geeksblue.inspection.ai.AliyunVisionService;
import com.geeksblue.inspection.ai.CleaningRuleEngine;
import com.geeksblue.inspection.ai.CleaningRuleEngine.Judgement;
import com.geeksblue.inspection.ai.DetectionResult;
import com.geeksblue.inspection.api.CheckResponse;
import com.geeksblue.inspection.api.ReviewItem;
import com.geeksblue.inspection.api.StatsResponse;
import com.geeksblue.inspection.api.StatsResponse.ClassroomStat;
import com.geeksblue.inspection.camera.CameraSnapshotService;
import com.geeksblue.inspection.domain.Classroom;
import com.geeksblue.inspection.domain.ClassroomRepository;
import com.geeksblue.inspection.domain.CleaningRecord;
import com.geeksblue.inspection.domain.CleaningRecordRepository;
import com.geeksblue.inspection.storage.PhotoStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 卫生巡查业务编排服务。
 *
 * <p>负责串联：图片落盘 → 调用 AI → 规则判定 → 写入数据库 → 老师复核流程。
 */
@Service
public class CleaningService {

    private static final Logger log = LoggerFactory.getLogger(CleaningService.class);

    private final PhotoStorage photoStorage;
    private final AliyunVisionService visionService;
    private final CleaningRuleEngine ruleEngine;
    private final CleaningRecordRepository repository;
    private final ClassroomLockService lockService;
    private final ClassroomRepository classroomRepository;
    private final CameraSnapshotService cameraService;

    public CleaningService(PhotoStorage photoStorage,
                           AliyunVisionService visionService,
                           CleaningRuleEngine ruleEngine,
                           CleaningRecordRepository repository,
                           ClassroomLockService lockService,
                           ClassroomRepository classroomRepository,
                           CameraSnapshotService cameraService) {
        this.photoStorage = photoStorage;
        this.visionService = visionService;
        this.ruleEngine = ruleEngine;
        this.repository = repository;
        this.lockService = lockService;
        this.classroomRepository = classroomRepository;
        this.cameraService = cameraService;
    }

    /**
     * 学生上传图片后的核心入口：
     * <ol>
     *   <li>学生图片落盘</li>
     *   <li>同步抓取教室监控快照（若该教室配置了 cameraUrl）</li>
     *   <li>两张图都跑 AI；结果合并（任一发现垃圾 → 转复核）</li>
     *   <li>规则判定 + 入库 + 释放教室占用锁</li>
     * </ol>
     * 监控不可用时整个流程不阻塞，只在 ai_detail 里追加 camera_unavailable 标记。
     */
    @Transactional
    public CheckResponse check(MultipartFile photo, String classroomId, String uploaderId) throws IOException {
        // 1) 学生上传的图片落盘
        Path studentPhoto = photoStorage.save(photo);

        // 2) 并行视角：从监控抓一帧（如果有配置）。失败时 cameraPath=null
        Path cameraPhoto = tryCaptureCameraSnapshot(classroomId);
        boolean cameraTried = false;
        boolean cameraOk = cameraPhoto != null;
        Classroom room = classroomRepository.findById(classroomId).orElse(null);
        if (room != null && room.getCameraUrl() != null && !room.getCameraUrl().isBlank()) {
            cameraTried = true;
        }

        // 3) 两张图都跑 AI；学生图为主，监控图为辅
        DetectionResult detection = visionService.detect(studentPhoto);
        if (cameraOk) {
            DetectionResult cameraDetection = visionService.detect(cameraPhoto);
            detection.mergeFrom(cameraDetection);
        }

        // 4) 规则引擎给出 pass / review
        Judgement judgement = ruleEngine.judge(detection);
        String reason = judgement.reason();
        if (cameraTried && !cameraOk) {
            // 监控应该可用但抓帧失败：在原因里标注一下，但不影响判定
            reason = reason + ", camera_unavailable";
        }

        // 5) 写入数据库
        CleaningRecord record = new CleaningRecord();
        record.setClassroomId(classroomId);
        record.setUploaderId(uploaderId);
        record.setPhotoPath(studentPhoto.toString());
        if (cameraOk) {
            record.setCameraPhotoPath(cameraPhoto.toString());
        }
        record.setAiResult(judgement.result());
        record.setAiDetail(reason);
        record.setCreatedAt(LocalDateTime.now());
        if (CleaningRuleEngine.PASS.equals(judgement.result())) {
            record.setFinalResult("pass");
            record.setReviewedAt(record.getCreatedAt());
        }
        record = repository.save(record);

        // 6) 释放占用锁，让别人立刻可以选这间教室
        lockService.release(classroomId, uploaderId);

        // 7) 返回给前端
        return new CheckResponse(judgement.result(), reason, String.valueOf(record.getId()));
    }

    /**
     * 抓取教室监控快照，落盘到 photoStorage.root()/camera/YYYY/MM/DD/uuid.jpg。
     * 教室无 cameraUrl 或抓帧失败时返回 null（由调用方决定如何处理）。
     */
    private Path tryCaptureCameraSnapshot(String classroomId) {
        Classroom room = classroomRepository.findById(classroomId).orElse(null);
        if (room == null || room.getCameraUrl() == null || room.getCameraUrl().isBlank()) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        Path target = photoStorage.root()
                .resolve("camera")
                .resolve(String.format("%04d", now.getYear()))
                .resolve(String.format("%02d", now.getMonthValue()))
                .resolve(String.format("%02d", now.getDayOfMonth()))
                .resolve(UUID.randomUUID() + ".jpg");
        boolean ok = cameraService.snapshot(room.getCameraUrl(), target);
        if (!ok) {
            log.warn("教室 {} 监控抓帧失败或未配置", classroomId);
            return null;
        }
        return target;
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
