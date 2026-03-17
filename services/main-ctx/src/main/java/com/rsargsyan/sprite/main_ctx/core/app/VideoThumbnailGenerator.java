package com.rsargsyan.sprite.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.AvifThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.WebpThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

public class VideoThumbnailGenerator {

  public static void run(String videoFilePath, Path configFolder, ThumbnailConfig config, Integer streamIndex) throws Exception {

    Files.createDirectories(configFolder);

    double fps = resolveFps(videoFilePath, streamIndex);

    generateThumbnails(videoFilePath, configFolder, config.resolution(), config.interval(), fps, streamIndex);

    Dimension dim = resolveImageSize(configFolder.resolve("thumbnail-000001.png"));

    int thumbnailsCount = countThumbnails(configFolder);
    int spriteR = config.spriteSize().rows();
    int spriteC = config.spriteSize().cols();
    int spriteS = spriteR * spriteC;

    generateSprites(configFolder, spriteC, spriteR, config);

    deleteThumbnails(configFolder);

    String spriteExt = config.format();
    if (thumbnailsCount <= spriteS) {
      Files.move(
          configFolder.resolve("sprite." + spriteExt),
          configFolder.resolve("sprite-0." + spriteExt),
          StandardCopyOption.REPLACE_EXISTING
      );
    }

    generateWebVtt(configFolder, thumbnailsCount, spriteS, spriteC, dim.width, dim.height,
        config.interval(), config.format());
  }

  private static double resolveFps(String videoUrl, Integer streamIndex) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(
        "ffprobe",
        "-v", "quiet",
        "-print_format", "json",
        "-show_streams",
        "-i", videoUrl
    );

    Process process = pb.start();
    String json = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("ffprobe failed (exit " + exitCode + ") for: " + videoUrl);
    }

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(json);

    JsonNode streams = root.get("streams");
    if (streams == null || !streams.isArray()) {
      throw new RuntimeException("ffprobe returned no streams for: " + videoUrl);
    }

    JsonNode videoStream;
    if (streamIndex != null) {
      if (streamIndex >= streams.size()) {
        throw new InvalidThumbnailConfigException(
            "Stream index " + streamIndex + " does not exist (video has " + streams.size() + " streams)");
      }
      videoStream = streams.get(streamIndex);
      if (!"video".equals(videoStream.path("codec_type").asText())) {
        throw new InvalidThumbnailConfigException(
            "Stream " + streamIndex + " is not a video stream (codec_type: " + videoStream.path("codec_type").asText() + ")");
      }
    } else {
      videoStream = null;
      for (JsonNode stream : streams) {
        if ("video".equals(stream.path("codec_type").asText())) {
          videoStream = stream;
          break;
        }
      }
      if (videoStream == null) {
        throw new RuntimeException("No video stream found in: " + videoUrl);
      }
    }

    String rate = videoStream.get("r_frame_rate").asText();
    String[] parts = rate.split("/");
    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
  }

  private static void generateThumbnails(String videoUrl,
                                         Path outputDir,
                                         int resolution,
                                         int interval,
                                         double fps,
                                         Integer streamIndex) throws Exception {

    String mapFlag = streamIndex != null ? "-map 0:" + streamIndex + " " : "";
    String cmd = String.format(
        "ffmpeg -i '%s' " + mapFlag + "-vf \"select='isnan(prev_selected_t)+gte(t-floor(prev_selected_t),%d)',scale=-2:%d,setpts=N/%f/TB\" thumbnail-%%06d.png",
        videoUrl,
        interval,
        resolution,
        fps
    );

    ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
    pb.directory(outputDir.toFile());
    pb.inheritIO();

    Process process = pb.start();
    process.waitFor();
  }

  private static Dimension resolveImageSize(Path imagePath) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(
        "identify",
        "-format",
        "%w %h",
        imagePath.toString()
    );

    Process process = pb.start();

    String output = new String(process.getInputStream().readAllBytes());
    String[] parts = output.trim().split(" ");

    if (parts.length != 2) throw new RuntimeException("Failed to resolve image size");

    return new Dimension(
        Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1])
    );
  }

  private static int countThumbnails(Path dir) throws IOException {

    try (Stream<Path> stream = Files.list(dir)) {
      return (int) stream
          .filter(p -> p.getFileName().toString().startsWith("thumbnail"))
          .count();
    }
  }

  private static void generateSprites(Path dir, int spriteC, int spriteR, ThumbnailConfig config) throws Exception {

    // Step 1: build montage as PNG (avoids animated WebP when writing multi-sprite output)
    String montageCmd = String.format(
        "magick montage -geometry +0+0 -tile %dx%d thumbnail-*.png sprite.png",
        spriteC, spriteR);
    new ProcessBuilder("bash", "-c", montageCmd).directory(dir.toFile()).start().waitFor();

    // Step 2: convert each sprite PNG to the target format individually
    StringBuilder convertFlags = new StringBuilder();
    convertFlags.append(String.format("-quality %d ", config.quality()));
    if (config instanceof WebpThumbnailConfig webp) {
      convertFlags.append(String.format("-define webp:preset=%s ", webp.preset()));
      if (webp.lossless()) convertFlags.append("-define webp:lossless=true ");
      convertFlags.append(String.format("-define webp:method=%d ", webp.method()));
    } else if (config instanceof AvifThumbnailConfig avif) {
      convertFlags.append(String.format("-define avif:speed=%d ", avif.speed()));
    }

    String convertCmd = String.format(
        "for f in sprite*.png; do magick \"$f\" %s\"${f%%.png}.%s\" && rm \"$f\"; done",
        convertFlags, config.format());
    new ProcessBuilder("bash", "-c", convertCmd).directory(dir.toFile()).start().waitFor();
  }

  private static void deleteThumbnails(Path dir) throws IOException {

    try (Stream<Path> stream = Files.list(dir)) {
      stream.filter(p -> p.getFileName().toString().startsWith("thumbnail"))
          .forEach(p -> {
            try { Files.delete(p); } catch (Exception ignored) {}
          });
    }
  }

  private static void generateWebVtt(Path dir,
                                     int thumbnailsCount,
                                     int spriteS,
                                     int spriteC,
                                     int width,
                                     int height,
                                     int interval,
                                     String format) throws Exception {

    Path vttFile = dir.resolve("thumbnails.vtt");

    StringBuilder vtt = new StringBuilder("WEBVTT\n\n");

    for (int i = 0; i < thumbnailsCount; i++) {

      int spriteNumber = i / spriteS;

      int start = i * interval;
      int end = (i + 1) * interval;

      int spritePositionX = (i % spriteC) * width;
      int spritePositionY = ((i % spriteS) / spriteC) * height;

      vtt.append(vttTimestamp(start))
          .append(" --> ")
          .append(vttTimestamp(end))
          .append("\n");

      vtt.append(String.format(
          "sprite-%d.%s#xywh=%d,%d,%d,%d\n\n",
          spriteNumber,
          format,
          spritePositionX,
          spritePositionY,
          width,
          height
      ));
    }

    Files.writeString(vttFile, vtt.toString(), StandardOpenOption.CREATE_NEW);
  }

  private static String vttTimestamp(int seconds) {

    int ms = seconds * 1000;

    int s = (ms / 1000) % 60;
    int m = (ms / 60000) % 60;
    int h = ms / 3600000;
    int milli = ms % 1000;

    return String.format("%02d:%02d:%02d.%03d", h, m, s, milli);
  }

  static class Dimension {
    int width;
    int height;

    Dimension(int w, int h) {
      width = w;
      height = h;
    }
  }
}
