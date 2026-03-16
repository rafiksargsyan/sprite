package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.JobSpecService;
import com.rsargsyan.sprite.main_ctx.core.app.dto.JobSpecCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.JobSpecDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/job-spec")
public class JobSpecController {

    private final JobSpecService jobSpecService;

    @Autowired
    public JobSpecController(JobSpecService jobSpecService) {
        this.jobSpecService = jobSpecService;
    }

    @PostMapping
    public ResponseEntity<JobSpecDTO> create(@RequestBody JobSpecCreationDTO req) {
        var userCtx = UserContextHolder.get();
        return new ResponseEntity<>(jobSpecService.create(userCtx.getAccountId(), req), HttpStatus.CREATED);
    }

    @GetMapping("/{jobSpecId}")
    public ResponseEntity<JobSpecDTO> findById(@PathVariable String jobSpecId) {
        var userCtx = UserContextHolder.get();
        return ResponseEntity.ok(jobSpecService.findById(userCtx.getAccountId(), jobSpecId));
    }
}
