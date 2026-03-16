package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "format", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = JpgThumbnailConfig.class, name = "jpg"),
    @JsonSubTypes.Type(value = WebpThumbnailConfig.class, name = "webp")
})
public sealed interface ThumbnailConfig permits JpgThumbnailConfig, WebpThumbnailConfig {
  String format();
  int resolution();
  SpriteSize spriteSize();
  int quality();
  int interval();
  String subfolderName();
}
