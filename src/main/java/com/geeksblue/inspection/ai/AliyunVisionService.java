package com.geeksblue.inspection.ai;

import com.aliyun.imagerecog20190930.Client;
import com.aliyun.imagerecog20190930.models.DetectObjectAdvanceRequest;
import com.aliyun.imagerecog20190930.models.DetectObjectResponse;
import com.aliyun.imagerecog20190930.models.DetectObjectResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 阿里云视觉智能（objectdet 目标检测）调用封装。
 *
 * <p>将检测到的目标按地面 / 桌面 / 讲台 / 垃圾桶四类聚合，供规则引擎消费。
 *
 * <p>配置项 {@code aliyun.mock=true} 时（默认），跳过真实 API 调用，
 * 返回空检测结果，便于本地开发和单元测试。
 */
@Service
public class AliyunVisionService {

    private static final Logger log = LoggerFactory.getLogger(AliyunVisionService.class);

    /** 阿里云 AccessKey ID */
    private final String accessKeyId;
    /** 阿里云 AccessKey Secret */
    private final String accessKeySecret;
    /** 视觉智能 API 接入点 */
    private final String endpoint;
    /** 是否走 mock 模式（不真实调用） */
    private final boolean mock;

    public AliyunVisionService(
            @Value("${aliyun.access-key-id:}") String accessKeyId,
            @Value("${aliyun.access-key-secret:}") String accessKeySecret,
            @Value("${aliyun.imagerecog-endpoint:imagerecog.cn-shanghai.aliyuncs.com}") String endpoint,
            @Value("${aliyun.mock:true}") boolean mock) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.endpoint = endpoint;
        this.mock = mock;
    }

    /**
     * 对指定图片调用阿里云目标检测。
     *
     * @param imagePath 服务器本地图片路径
     * @return 检测结果（按区域分类）
     */
    public DetectionResult detect(Path imagePath) {
        // 没配置 Key 或显式 mock，直接返回空结果（判定会走 pass）
        if (mock || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            log.info("阿里云视觉处于 mock 模式，返回空检测结果，图片={}", imagePath);
            return new DetectionResult();
        }
        try (InputStream in = Files.newInputStream(imagePath)) {
            // 构造客户端
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint(endpoint);
            Client client = new Client(config);

            // 以输入流方式上传图片
            DetectObjectAdvanceRequest request = new DetectObjectAdvanceRequest();
            request.setImageURLObject(in);

            DetectObjectResponse response = client.detectObjectAdvance(request, new RuntimeOptions());
            return mapResponse(response);
        } catch (Exception e) {
            log.error("阿里云视觉调用失败，图片={}", imagePath, e);
            // 容错策略：AI 故障时不阻塞上传，转人工复核
            DetectionResult fallback = new DetectionResult();
            fallback.getFloorBigTrash().add("ai_unavailable");
            return fallback;
        }
    }

    /**
     * 将阿里云返回的目标对象映射到四个区域桶。
     *
     * <p>当前是基于目标 type 的简化映射，后续可替换为更精细的位置/面积/置信度规则。
     */
    private DetectionResult mapResponse(DetectObjectResponse response) {
        DetectionResult result = new DetectionResult();
        if (response == null || response.getBody() == null || response.getBody().getData() == null) {
            return result;
        }
        List<DetectObjectResponseBody.DetectObjectResponseBodyDataElements> elements =
                response.getBody().getData().getElements();
        if (elements == null) {
            return result;
        }
        for (var el : elements) {
            String type = el.getType() == null ? "" : el.getType().toLowerCase(Locale.ROOT);
            Float score = el.getScore() == null ? 0f : el.getScore();
            // 置信度阈值：低于 0.6 视为噪音，避免误判
            if (score < 0.6f) {
                continue;
            }
            switch (type) {
                // 地面区域：大件垃圾、纸箱、垃圾袋
                case "trash", "garbage", "box", "bag" -> result.getFloorBigTrash().add(type);
                // 桌面区域：饮料瓶、杯子、易拉罐
                case "bottle", "cup", "can" -> result.getDeskTrash().add(type);
                // 讲台区域：堆积物 / 成摞书本
                case "pile", "stack", "books" -> result.getPodiumPiles().add(type);
                // 垃圾桶区域：溢出
                case "overflow", "trashcan" -> result.getBinOverflow().add(type);
                default -> { /* 其它类型忽略 */ }
            }
        }
        return result;
    }
}
