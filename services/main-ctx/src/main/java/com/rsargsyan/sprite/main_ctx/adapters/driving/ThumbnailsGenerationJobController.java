package com.rsargsyan.sprite.main_ctx.adapters.driving;

import com.rsargsyan.sprite.main_ctx.adapters.driving.dto.ThumbnailsGenerationJobCreationDTO;
import com.rsargsyan.sprite.main_ctx.adapters.driving.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/thumbnails-generation-job")
public class ThumbnailsGenerationJobController {

    private final ThumbnailsGenerationJobService thumbnailsGenerationJobService;

    @Autowired
    public ThumbnailsGenerationJobController(ThumbnailsGenerationJobService thumbnailsGenerationJobService) {
        this.thumbnailsGenerationJobService = thumbnailsGenerationJobService;
    }

    @PostMapping("/")
    public ResponseEntity<ThumbnailsGenerationJobDTO> createThumbnailsGenerationJob(
        @RequestBody ThumbnailsGenerationJobCreationDTO req
    ) {
        ThumbnailsGenerationJob job = thumbnailsGenerationJobService.create(req.getVideoURL());
        return new ResponseEntity<>(new ThumbnailsGenerationJobDTO(job.getId(),
            job.getVideoURL()), HttpStatus.CREATED);
    }

}
