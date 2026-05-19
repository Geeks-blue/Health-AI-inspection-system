package com.geeksblue.inspection.api;

/**
 * Response for POST /api/cleaning/check.
 */
public class CheckResponse {

    private String result;
    private String reason;
    private String recordId;

    public CheckResponse() {}

    public CheckResponse(String result, String reason, String recordId) {
        this.result = result;
        this.reason = reason;
        this.recordId = recordId;
    }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
}
