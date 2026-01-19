package com.example.backend.controller;

import com.example.backend.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantDebugController {

    @GetMapping("/api/tenant")
    public String getTenant() {
        return TenantContext.getTenantId();
    }
}
