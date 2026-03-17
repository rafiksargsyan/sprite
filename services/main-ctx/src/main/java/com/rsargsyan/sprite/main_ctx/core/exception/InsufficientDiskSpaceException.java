package com.rsargsyan.sprite.main_ctx.core.exception;

public class InsufficientDiskSpaceException extends DomainException {
  public InsufficientDiskSpaceException(long freeBytes, long requiredBytes) {
    super("Insufficient disk space: %s free, %s required"
        .formatted(humanReadableBytes(freeBytes), humanReadableBytes(requiredBytes)));
  }

  private static String humanReadableBytes(long bytes) {
    if (bytes >= 1_073_741_824L) return "%.1f GB".formatted(bytes / 1_073_741_824.0);
    if (bytes >= 1_048_576L) return "%.1f MB".formatted(bytes / 1_048_576.0);
    return "%d KB".formatted(bytes / 1024);
  }
}
