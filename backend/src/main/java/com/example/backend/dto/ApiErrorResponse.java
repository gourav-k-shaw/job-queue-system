package com.example.backend.dto;

import java.time.Instant;

public class ApiErrorResponse {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String tenantId;

    public ApiErrorResponse(int status, String error, String message, String path, String tenantId) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.tenantId = tenantId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getTenantId() {
        return tenantId;
    }
}
