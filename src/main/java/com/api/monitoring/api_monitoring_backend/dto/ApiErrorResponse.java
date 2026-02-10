package com.api.monitoring.api_monitoring_backend.dto;

public class ApiErrorResponse {
    private boolean success;
    private String message;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
