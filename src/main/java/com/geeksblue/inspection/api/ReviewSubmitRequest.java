package com.geeksblue.inspection.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/cleaning/review/submit 接口的请求体。
 */
public class ReviewSubmitRequest {

    /** 待复核记录 ID */
    @NotNull
    private Long recordId;

    /** 复核结果：pass 或 fail */
    @NotBlank
    private String result;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
