package com.rsargsyan.sprite.main_ctx.core.app.dto;

import lombok.Value;

@Value
public class ThumbnailsGenerationJobCreationDTO {
  String videoURL;
  String jobSpecId;
  Integer streamIndex;
  boolean preview;
}
