package com.rsargsyan.sprite.main_ctx.core.exception;

import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;

public class IllegalJobStateTransitionException extends DomainException {
  public IllegalJobStateTransitionException(ThumbnailsGenerationJob.Status from, ThumbnailsGenerationJob.Status to) {
    super("Illegal job state transition: %s → %s".formatted(from, to));
  }
}
