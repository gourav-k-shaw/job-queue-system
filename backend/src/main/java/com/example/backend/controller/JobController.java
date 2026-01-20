package com.example.backend.controller;

import com.example.backend.dto.CreateJobRequest;
import com.example.backend.dto.CreateJobResponse;
import com.example.backend.dto.JobResponse;
import com.example.backend.dto.JobSummaryResponse;
import com.example.backend.service.JobService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public CreateJobResponse submitJob(@RequestBody CreateJobRequest request) {
        return jobService.submitJob(request);
    }

    @GetMapping("/{jobId}")
    public JobResponse getJobById(@PathVariable UUID jobId) {
        return jobService.getJobById(jobId);
    }

    @GetMapping
    public Page<JobResponse> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.listJobs(status, page, size);
    }

    @GetMapping("/dlq")
    public Page<JobResponse> listDlqJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return jobService.listDlqJobs(page, size);
    }

    @GetMapping("/summary")
    public JobSummaryResponse getSummary() {
        return jobService.getSummary();
    }
}
