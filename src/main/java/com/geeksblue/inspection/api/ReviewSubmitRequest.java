package com.geeksblue.inspection.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/cleaning/review/submit.
 */
public class ReviewSubmitRequest {

    @NotNull
    private Long recordId;

    /** "pass" or "fail". */
    @NotBlank
    private String result;

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
