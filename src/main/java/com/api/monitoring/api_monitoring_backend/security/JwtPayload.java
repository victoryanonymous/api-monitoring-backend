package com.api.monitoring.api_monitoring_backend.security;

public class JwtPayload {
    private final String userId;
    private final String loginId;

    public JwtPayload(String userId, String loginId) {
        this.userId = userId;
        this.loginId = loginId;
    }

    public String getUserId() {
        return userId;
    }

    public String getLoginId() {
        return loginId;
    }
}
