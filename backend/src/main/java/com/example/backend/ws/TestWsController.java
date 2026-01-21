package com.example.backend.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ws-test")
public class TestWsController {

    private final SimpMessagingTemplate messagingTemplate;

    public TestWsController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/ping")
    public String ping(@RequestParam String tenantId) {
        messagingTemplate.convertAndSend(
                "/topic/tenant/" + tenantId + "/summary",
                Map.of("message", "hello from backend"));
        return "sent";
    }
}
