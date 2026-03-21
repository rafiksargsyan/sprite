package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record AvifThumbnailConfigRequest(int resolution, SpriteSizeRequest spriteSize, int quality, int interval, String folderName)
    implements ThumbnailConfigRequest {}
