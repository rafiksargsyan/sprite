package com.rsargsyan.sprite.main_ctx.core.app.dto;

public record BlurhashThumbnailConfigRequest(int resolution, int interval, int componentsX, int componentsY, String folderName)
    implements ThumbnailConfigRequest {}
