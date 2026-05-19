package com.geeksblue.inspection.api;

/**
 * POST /api/cleaning/check 接口的返回体。
 */
public class CheckResponse {

    /** 判定结果：pass / review */
    private String result;

    /** 判定原因（人类可读） */
    private String reason;

    /** 记录主键，字符串形式，便于前端处理 */
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
