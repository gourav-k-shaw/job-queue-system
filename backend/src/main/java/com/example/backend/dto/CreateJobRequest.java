package com.example.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class CreateJobRequest {

    private JsonNode payload;
    private String idempotencyKey;

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
