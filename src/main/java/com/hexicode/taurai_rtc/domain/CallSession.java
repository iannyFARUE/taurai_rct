package com.hexicode.taurai_rtc.domain;

public class CallSession {
    private String callId;
    private String fromUserId;
    private String toUserId;
    private String status;
    private long createdAt;
    
    // Constructors, getters and setters
    public CallSession() {}
    
    public CallSession(String callId, String fromUserId, String toUserId, String status) {
        this.callId = callId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = status;
        this.createdAt = System.currentTimeMillis();
    }
    
    public String getCallId() { return callId; }
    public void setCallId(String callId) { this.callId = callId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
