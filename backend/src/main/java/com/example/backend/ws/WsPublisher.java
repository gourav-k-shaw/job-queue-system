package com.example.backend.ws;

import com.example.backend.dto.JobSummaryResponse;
import com.example.backend.model.JobEntity;
import com.example.backend.repository.JobRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WsPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final JobRepository jobRepository;

    public WsPublisher(SimpMessagingTemplate messagingTemplate, JobRepository jobRepository) {
        this.messagingTemplate = messagingTemplate;
        this.jobRepository = jobRepository;
    }

    public void publishJobUpdate(JobEntity job) {
        String tenantId = job.getTenantId();

        JobUpdatedEvent event = new JobUpdatedEvent(
                job.getId(),
                tenantId,
                job.getStatus(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getUpdatedAt());

        messagingTemplate.convertAndSend(
                "/topic/tenant/" + tenantId + "/jobs",
                event);
    }

    public void publishSummaryUpdate(String tenantId, JobSummaryResponse summary) {
        SummaryUpdatedEvent event = new SummaryUpdatedEvent(
                tenantId,
                summary.getPending(),
                summary.getRunning(),
                summary.getDone(),
                summary.getDlq());

        messagingTemplate.convertAndSend(
                "/topic/tenant/" + tenantId + "/summary",
                event);
    }
}
