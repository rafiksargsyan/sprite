package com.rsargsyan.sprite.main_ctx.core.app.dto;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FailureReason;

public enum JobFailureReason {
  VIDEO_TOO_LARGE,
  VIDEO_NOT_ACCESSIBLE,
  INVALID_STREAM_INDEX,
  SERVER_ERROR;

  public static JobFailureReason from(FailureReason reason) {
    return switch (reason) {
      case VIDEO_TOO_LARGE -> VIDEO_TOO_LARGE;
      case VIDEO_NOT_ACCESSIBLE -> VIDEO_NOT_ACCESSIBLE;
      case INVALID_STREAM_INDEX -> INVALID_STREAM_INDEX;
      default -> SERVER_ERROR;
    };
  }
}
