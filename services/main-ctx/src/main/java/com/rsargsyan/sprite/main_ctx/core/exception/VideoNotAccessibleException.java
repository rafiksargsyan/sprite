package com.rsargsyan.sprite.main_ctx.core.exception;

public class VideoNotAccessibleException extends DomainException {
  public VideoNotAccessibleException(String message) {
    super(message);
  }

  public VideoNotAccessibleException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
