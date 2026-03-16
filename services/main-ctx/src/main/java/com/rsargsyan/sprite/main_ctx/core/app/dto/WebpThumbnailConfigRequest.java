package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record WebpThumbnailConfigRequest(int resolution, SpriteSizeRequest spriteSize, int quality, int method, boolean lossless, int interval)
    implements ThumbnailConfigRequest {}
