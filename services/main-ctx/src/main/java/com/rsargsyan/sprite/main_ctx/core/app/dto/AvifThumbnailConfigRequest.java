package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record AvifThumbnailConfigRequest(int resolution, SpriteSizeRequest spriteSize, int quality, int interval, int speed, String folderName)
    implements ThumbnailConfigRequest {}
