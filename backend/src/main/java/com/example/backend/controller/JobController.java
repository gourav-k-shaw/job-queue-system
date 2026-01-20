package com.example.backend.controller;

import com.example.backend.dto.CreateJobRequest;
import com.example.backend.dto.CreateJobResponse;
import com.example.backend.service.JobService;
import org.springframework.web.bind.annotation.*;

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
}
