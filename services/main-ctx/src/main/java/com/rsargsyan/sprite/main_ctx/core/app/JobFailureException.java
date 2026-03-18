package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FailureReason;

class JobFailureException extends RuntimeException {
  final FailureReason reason;

  JobFailureException(FailureReason reason, Throwable cause) {
    super(reason.name(), cause);
    this.reason = reason;
  }
}
