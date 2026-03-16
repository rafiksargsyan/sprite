package com.rsargsyan.sprite.main_ctx.core.app.dto;

import lombok.Value;

import java.util.List;

@Value
public class JobSpecCreationDTO {
  String name;
  String description;
  List<ThumbnailConfigRequest> configs;
}
