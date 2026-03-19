package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

public record ConfigProcessingStats(
    String folderName,
    long extractionMs,
    long postProcessingMs
) {}
