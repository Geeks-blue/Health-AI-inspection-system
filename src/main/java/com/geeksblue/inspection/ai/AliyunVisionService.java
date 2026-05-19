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
 * Calls Alibaba Cloud Vision Intelligence (objectdet) and maps detected
 * objects to the four zone buckets consumed by the rule engine.
 *
 * Set {@code aliyun.mock=true} (the default) to return a deterministic
 * pass-result without making any network calls, which is convenient for
 * local development and tests.
 */
@Service
public class AliyunVisionService {

    private static final Logger log = LoggerFactory.getLogger(AliyunVisionService.class);

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;
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

    public DetectionResult detect(Path imagePath) {
        if (mock || accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            log.info("Aliyun vision in mock mode, returning empty detection for {}", imagePath);
            return new DetectionResult();
        }
        try (InputStream in = Files.newInputStream(imagePath)) {
            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret)
                    .setEndpoint(endpoint);
            Client client = new Client(config);

            DetectObjectAdvanceRequest request = new DetectObjectAdvanceRequest();
            request.setImageURLObject(in);

            DetectObjectResponse response = client.detectObjectAdvance(request, new RuntimeOptions());
            return mapResponse(response);
        } catch (Exception e) {
            log.error("Aliyun vision detection failed for {}", imagePath, e);
            // Fail-open: an AI outage should not block uploads; route to manual review.
            DetectionResult fallback = new DetectionResult();
            fallback.getFloorBigTrash().add("ai_unavailable");
            return fallback;
        }
    }

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
            // Lenient thresholds — only obvious detections trigger review.
            if (score < 0.6f) {
                continue;
            }
            switch (type) {
                case "trash", "garbage", "box", "bag" -> result.getFloorBigTrash().add(type);
                case "bottle", "cup", "can" -> result.getDeskTrash().add(type);
                case "pile", "stack", "books" -> result.getPodiumPiles().add(type);
                case "overflow", "trashcan" -> result.getBinOverflow().add(type);
                default -> { /* ignore everything else */ }
            }
        }
        return result;
    }
}
