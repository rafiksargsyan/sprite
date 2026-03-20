package com.rsargsyan.sprite.main_ctx.core.domain.valueobject;

public enum VideoCodec {
  H264("h264", 1.0),
  H265("hevc", 1.8),
  AV1("av1", 2.5),
  OTHER(null, 1.5);

  public final double factor;
  private final String ffprobeName;

  VideoCodec(String ffprobeName, double factor) {
    this.ffprobeName = ffprobeName;
    this.factor = factor;
  }

  public static VideoCodec fromFfprobeName(String name) {
    if (name == null) return OTHER;
    for (VideoCodec c : values()) {
      if (c.ffprobeName != null && c.ffprobeName.equalsIgnoreCase(name)) return c;
    }
    return OTHER;
  }
}
