package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/thumbnails-generation-job")
public class ThumbnailsGenerationJobController {

    private final ThumbnailsGenerationJobService thumbnailsGenerationJobService;

    @Autowired
    public ThumbnailsGenerationJobController(ThumbnailsGenerationJobService thumbnailsGenerationJobService) {
        this.thumbnailsGenerationJobService = thumbnailsGenerationJobService;
    }

    @GetMapping
    public ResponseEntity<Page<ThumbnailsGenerationJobDTO>> findAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        var userCtx = UserContextHolder.get();
        return ResponseEntity.ok(thumbnailsGenerationJobService.findAll(userCtx.getAccountId(), page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThumbnailsGenerationJobDTO> findById(@PathVariable String id) {
        var userCtx = UserContextHolder.get();
        return ResponseEntity.ok(thumbnailsGenerationJobService.findById(userCtx.getAccountId(), id));
    }

    @GetMapping("/limits")
    public ResponseEntity<Map<String, Long>> getLimits() {
        return ResponseEntity.ok(Map.of("maxFileSizeBytes", thumbnailsGenerationJobService.getMaxFileSizeBytes()));
    }

    @GetMapping("/{id}/preview/{configFolderName}")
    public ResponseEntity<Map<String, String>> getPreviewFiles(
        @PathVariable String id,
        @PathVariable String configFolderName
    ) {
        var userCtx = UserContextHolder.get();
        return ResponseEntity.ok(thumbnailsGenerationJobService.getPreviewFiles(userCtx.getAccountId(), id, configFolderName));
    }

    @GetMapping(value = "/{id}/preview/{configFolderName}/vtt", produces = "text/vtt")
    public ResponseEntity<String> getPreviewVtt(
        @PathVariable String id,
        @PathVariable String configFolderName
    ) {
        var userCtx = UserContextHolder.get();
        return ResponseEntity.ok(thumbnailsGenerationJobService.getPreviewVtt(userCtx.getAccountId(), id, configFolderName));
    }

    @PostMapping
    public ResponseEntity<ThumbnailsGenerationJobDTO> createThumbnailsGenerationJob(
        @RequestBody ThumbnailsGenerationJobCreationDTO req
    ) {
        var userCtx = UserContextHolder.get();
        ThumbnailsGenerationJobDTO job = thumbnailsGenerationJobService.create(userCtx.getAccountId(), req);
        return new ResponseEntity<>(job, HttpStatus.CREATED);
    }

}
