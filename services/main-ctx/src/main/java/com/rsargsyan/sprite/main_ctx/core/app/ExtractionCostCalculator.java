package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.VideoCodec;

public class ExtractionCostCalculator {

  private static final double BASE_INPUT_RES = 720.0;

  public static double calculate(int durationSec, VideoCodec codec, int inputRes) {
    double inputResFactor = Math.pow(inputRes / BASE_INPUT_RES, 2);
    return durationSec * codec.factor * inputResFactor;
  }
}
