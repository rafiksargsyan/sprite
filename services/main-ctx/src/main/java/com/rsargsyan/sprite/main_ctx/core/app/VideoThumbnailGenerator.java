package com.rsargsyan.sprite.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.AvifThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.BlurhashThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ConfigProcessingStats;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.VideoCodec;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.WebpThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.InvalidThumbnailConfigException;
import io.trbl.blurhash.BlurHash;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class VideoThumbnailGenerator {

  public static VideoProbeResult probe(String videoUrl, Integer streamIndex) throws Exception {
    return probeVideo(videoUrl, streamIndex);
  }

  public static ConfigProcessingStats run(String videoFilePath, Path configFolder, ThumbnailConfig config,
                                          Integer streamIndex, int threads, double fps) throws Exception {

    Files.createDirectories(configFolder);

    int resolution = config.resolution();
    int jpegQuality = config instanceof BlurhashThumbnailConfig ? 8 : 2;

    long extractionStart = System.currentTimeMillis();
    generateThumbnails(videoFilePath, configFolder, resolution, config.interval(), fps, streamIndex, threads, jpegQuality);
    long extractionMs = System.currentTimeMillis() - extractionStart;

    long postProcessingStart = System.currentTimeMillis();
    if (config instanceof BlurhashThumbnailConfig blurhash) {
      generateBlurhashVtt(configFolder, blurhash.interval(), blurhash.componentsX(), blurhash.componentsY());
    } else {
      Dimension dim = resolveImageSize(configFolder.resolve("thumbnail-000001.jpg"));
      int thumbnailsCount = countThumbnails(configFolder);
      int spriteR = config.spriteSize().rows();
      int spriteC = config.spriteSize().cols();
      int spriteS = spriteR * spriteC;

      generateSprites(configFolder, spriteC, spriteR, spriteS, thumbnailsCount, config);

      generateWebVtt(configFolder, thumbnailsCount, spriteS, spriteC, dim.width, dim.height,
          config.interval(), config.format());
    }
    long postProcessingMs = System.currentTimeMillis() - postProcessingStart;
    deleteThumbnails(configFolder);

    return new ConfigProcessingStats(config.folderName(), extractionMs, postProcessingMs);
  }

  private static VideoProbeResult probeVideo(String videoUrl, Integer streamIndex) throws Exception {

    boolean isUrl = videoUrl.startsWith("http://") || videoUrl.startsWith("https://") || videoUrl.startsWith("rtmp://") || videoUrl.startsWith("rtsp://");
    List<String> cmd = new ArrayList<>(List.of("ffprobe", "-v", "error", "-print_format", "json", "-show_streams", "-show_format"));
    if (isUrl) {
      cmd.addAll(List.of("-timeout", "30000000", "-rw_timeout", "30000000"));
    }
    cmd.addAll(List.of("-i", videoUrl));
    ProcessBuilder pb = new ProcessBuilder(cmd);

    Process process = pb.start();
    String json = new String(process.getInputStream().readAllBytes());
    String stderr = new String(process.getErrorStream().readAllBytes());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("ffprobe failed (exit " + exitCode + "): " + stderr.strip());
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
    double fps = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);

    String codecName = videoStream.path("codec_name").asText(null);
    VideoCodec codec = VideoCodec.fromFfprobeName(codecName);

    int inputRes = videoStream.path("height").asInt(720);

    double durationSec = root.path("format").path("duration").asDouble(0);

    return new VideoProbeResult(fps, (int) durationSec, codec, inputRes);
  }

  private static void generateThumbnails(String videoUrl,
                                         Path outputDir,
                                         int resolution,
                                         int interval,
                                         double fps,
                                         Integer streamIndex,
                                         int threads,
                                         int jpegQuality) throws Exception {

    String mapFlag = streamIndex != null ? "-map 0:" + streamIndex + " " : "";
    boolean isUrl = videoUrl.startsWith("http://") || videoUrl.startsWith("https://") || videoUrl.startsWith("rtmp://") || videoUrl.startsWith("rtsp://");
    String networkFlags = isUrl ? "-timeout 30000000 -rw_timeout 30000000 " : "";
    String cmd = String.format(
        "ffmpeg " + networkFlags + "-threads %d -skip_frame noref -i '%s' " + mapFlag + "-vf \"select='isnan(prev_selected_t)+gte(t-floor(prev_selected_t),%d)',scale=-2:%d,setpts=N/%f/TB\" -vsync vfr -q:v %d thumbnail-%%06d.jpg",
        threads,
        videoUrl,
        interval,
        resolution,
        fps,
        jpegQuality
    );

    runChecked(cmd, outputDir, "ffmpeg");
  }

  private static Dimension resolveImageSize(Path imagePath) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(
        "ffprobe", "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=width,height",
        "-of", "csv=p=0",
        imagePath.toString()
    );

    Process process = pb.start();
    String output = new String(process.getInputStream().readAllBytes());
    int exitCode = process.waitFor();

    if (exitCode != 0) throw new RuntimeException("ffprobe failed to resolve image size");

    String[] parts = output.trim().split(",");
    if (parts.length != 2) throw new RuntimeException("Failed to resolve image size");

    return new Dimension(
        Integer.parseInt(parts[0].trim()),
        Integer.parseInt(parts[1].trim())
    );
  }

  private static int countThumbnails(Path dir) throws IOException {

    try (Stream<Path> stream = Files.list(dir)) {
      return (int) stream
          .filter(p -> p.getFileName().toString().startsWith("thumbnail-"))
          .count();
    }
  }

  private static void generateSprites(Path dir, int spriteC, int spriteR, int spriteS,
                                      int thumbnailCount, ThumbnailConfig config) throws Exception {

    int spriteCount = (thumbnailCount + spriteS - 1) / spriteS;

    for (int s = 0; s < spriteCount; s++) {
      int startNumber = s * spriteS + 1;
      int batchSize = Math.min(spriteS, thumbnailCount - s * spriteS);
      Path spriteFile = dir.resolve("sprite-" + s + "." + config.format());

      List<String> cmd = new ArrayList<>(List.of(
          "ffmpeg", "-y",
          "-start_number", String.valueOf(startNumber),
          "-i", "thumbnail-%06d.jpg",
          "-vf", "trim=start_frame=0:end_frame=" + batchSize + ",tile=" + spriteC + "x" + spriteR,
          "-frames:v", "1"
      ));

      if (config instanceof WebpThumbnailConfig webp) {
        cmd.addAll(List.of("-c:v", "libwebp"));
        cmd.addAll(List.of("-quality", String.valueOf(webp.quality())));
        cmd.addAll(List.of("-lossless", "0"));
        cmd.addAll(List.of("-compression_level", String.valueOf(webp.method())));
      } else if (config instanceof AvifThumbnailConfig avif) {
        int crf = (100 - avif.quality()) * 63 / 100;
        cmd.addAll(List.of("-c:v", "libaom-av1"));
        cmd.addAll(List.of("-crf", String.valueOf(crf)));
        cmd.addAll(List.of("-cpu-used", "6"));
      } else {
        int q = Math.max(1, Math.round((100 - config.quality()) * 31.0f / 100));
        cmd.addAll(List.of("-q:v", String.valueOf(q)));
      }

      cmd.addAll(List.of("-update", "1", spriteFile.toString()));
      runChecked(cmd, dir, "ffmpeg sprite");
    }
  }

  private static void generateBlurhashVtt(Path dir, int interval, int componentsX, int componentsY) throws Exception {

    List<Path> thumbnails;
    try (Stream<Path> stream = Files.list(dir)) {
      thumbnails = stream
          .filter(p -> p.getFileName().toString().startsWith("thumbnail-"))
          .sorted()
          .toList();
    }

    Path vttFile = dir.resolve("thumbnails.vtt");
    StringBuilder vtt = new StringBuilder("WEBVTT\n\n");

    for (int i = 0; i < thumbnails.size(); i++) {
      BufferedImage img = ImageIO.read(thumbnails.get(i).toFile());
      int width = img.getWidth();
      int height = img.getHeight();
      int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);
      String hash = BlurHash.encode(pixels, width, height, componentsX, componentsY);

      int start = i * interval;
      int end = (i + 1) * interval;

      vtt.append(vttTimestamp(start))
          .append(" --> ")
          .append(vttTimestamp(end))
          .append("\n")
          .append(hash).append(" ").append(width).append(" ").append(height)
          .append("\n\n");
    }

    Files.writeString(vttFile, vtt.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private static void deleteThumbnails(Path dir) throws IOException {

    try (Stream<Path> stream = Files.list(dir)) {
      stream.filter(p -> p.getFileName().toString().startsWith("thumbnail-"))
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

    Files.writeString(vttFile, vtt.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  private static String vttTimestamp(int seconds) {

    int ms = seconds * 1000;

    int s = (ms / 1000) % 60;
    int m = (ms / 60000) % 60;
    int h = ms / 3600000;
    int milli = ms % 1000;

    return String.format("%02d:%02d:%02d.%03d", h, m, s, milli);
  }

  private static void runChecked(List<String> cmd, Path dir, String name) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(cmd).directory(dir.toFile());
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    Process process = pb.start();
    String stderr = new String(process.getErrorStream().readAllBytes());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(name + " failed (exit " + exitCode + "): " + stderr.strip());
    }
  }

  private static void runChecked(String cmd, Path dir, String name) throws Exception {
    ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd).directory(dir.toFile());
    pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
    Process process = pb.start();
    String stderr = new String(process.getErrorStream().readAllBytes());
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException(name + " failed (exit " + exitCode + "): " + stderr.strip());
    }
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
