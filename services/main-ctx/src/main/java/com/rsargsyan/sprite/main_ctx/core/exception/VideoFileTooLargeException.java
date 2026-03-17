package com.rsargsyan.sprite.main_ctx.core.exception;

public class VideoFileTooLargeException extends DomainException {
  public VideoFileTooLargeException(long actualBytes, long maxBytes) {
    super("Video file size (%s) exceeds the maximum allowed size of %s"
        .formatted(humanReadableBytes(actualBytes), humanReadableBytes(maxBytes)));
  }

  private static String humanReadableBytes(long bytes) {
    if (bytes >= 1_073_741_824L) return "%.1f GB".formatted(bytes / 1_073_741_824.0);
    if (bytes >= 1_048_576L) return "%.1f MB".formatted(bytes / 1_048_576.0);
    return "%d KB".formatted(bytes / 1024);
  }
}
