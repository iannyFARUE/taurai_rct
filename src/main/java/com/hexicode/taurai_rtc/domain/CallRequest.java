package com.hexicode.taurai_rtc.domain;

public class CallRequest {
    private String fromUserId;
    private String toUserId;
    
    // Getters and setters
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
}
