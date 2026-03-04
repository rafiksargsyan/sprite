package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/thumbnails-generation-job")
public class ThumbnailsGenerationJobController {

    private final ThumbnailsGenerationJobService thumbnailsGenerationJobService;

    @Autowired
    public ThumbnailsGenerationJobController(ThumbnailsGenerationJobService thumbnailsGenerationJobService) {
        this.thumbnailsGenerationJobService = thumbnailsGenerationJobService;
    }

    @PostMapping
    public ResponseEntity<ThumbnailsGenerationJobDTO> createThumbnailsGenerationJob(
        @RequestBody ThumbnailsGenerationJobCreationDTO req
    ) {
        ThumbnailsGenerationJobDTO job = thumbnailsGenerationJobService.create(req.getVideoURL());
        return new ResponseEntity<>(job, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/touch")
    public ResponseEntity<ThumbnailsGenerationJobDTO> touch(@PathVariable String id) {
        String accountId = ""; //TODO get
        ThumbnailsGenerationJobDTO job = thumbnailsGenerationJobService.touch(accountId, id);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }
}
