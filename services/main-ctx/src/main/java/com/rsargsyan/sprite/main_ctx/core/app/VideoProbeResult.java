package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.VideoCodec;

public record VideoProbeResult(double fps, int durationSec, VideoCodec codec, int inputRes) {}
