package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record JpgThumbnailConfigRequest(int resolution, SpriteSizeRequest spriteSize, int quality, int interval)
    implements ThumbnailConfigRequest {}
