package com.hexicode.taurai_rtc.domain;

public class UserInfo {
    private String userId;
    private String username;
    private String fullName;
    private String email;
    
    public UserInfo(String userId, String username, String fullName, String email) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }
    
    // Getters and setters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
}
