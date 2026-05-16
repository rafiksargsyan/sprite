package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import lombok.Value;

import java.util.List;

@Value
public class ThumbnailsGenerationJobCreationDTO {
  String videoURL;
  String jobSpecId;
  List<ThumbnailConfig> configs;
  Integer streamIndex;
  boolean preview;
}
