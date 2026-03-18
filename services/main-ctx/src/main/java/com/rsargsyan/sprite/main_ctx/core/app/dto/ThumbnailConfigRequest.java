package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "format")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JpgThumbnailConfigRequest.class, name = "jpg"),
    @JsonSubTypes.Type(value = WebpThumbnailConfigRequest.class, name = "webp"),
    @JsonSubTypes.Type(value = AvifThumbnailConfigRequest.class, name = "avif"),
    @JsonSubTypes.Type(value = BlurhashThumbnailConfigRequest.class, name = "blurhash")
})
public sealed interface ThumbnailConfigRequest permits JpgThumbnailConfigRequest, WebpThumbnailConfigRequest,
    AvifThumbnailConfigRequest, BlurhashThumbnailConfigRequest {}
