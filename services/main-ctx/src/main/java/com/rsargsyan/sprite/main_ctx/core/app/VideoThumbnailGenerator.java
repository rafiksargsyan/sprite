package com.rsargsyan.sprite.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.AvifThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.BlurhashThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
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

  public static void run(String videoFilePath, Path configFolder, ThumbnailConfig config, Integer streamIndex, int threads) throws Exception {

    Files.createDirectories(configFolder);

    double fps = resolveFps(videoFilePath, streamIndex);

    int resolution = config instanceof BlurhashThumbnailConfig ? 32 : config.resolution();
    int jpegQuality = config instanceof BlurhashThumbnailConfig ? 8 : 2;
    generateThumbnails(videoFilePath, configFolder, resolution, config.interval(), fps, streamIndex, threads, jpegQuality);

    if (config instanceof BlurhashThumbnailConfig blurhash) {
      generateBlurhashVtt(configFolder, blurhash.interval(), blurhash.componentsX(), blurhash.componentsY());
    } else {
      Dimension dim = resolveImageSize(configFolder.resolve("thumbnail-000001.jpg"));
      int thumbnailsCount = countThumbnails(configFolder);
      int spriteR = config.spriteSize().rows();
      int spriteC = config.spriteSize().cols();
      int spriteS = spriteR * spriteC;
      generateSprites(configFolder, spriteC, spriteR, spriteS, config);
      generateWebVtt(configFolder, thumbnailsCount, spriteS, spriteC, dim.width, dim.height,
          config.interval(), config.format());
    }

    deleteThumbnails(configFolder);
  }

  private static double resolveFps(String videoUrl, Integer streamIndex) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(
        "ffprobe",
        "-v", "error",
        "-print_format", "json",
        "-show_streams",
        "-i", videoUrl
    );

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
    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
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
    String cmd = String.format(
        "ffmpeg -threads %d -skip_frame noref -i '%s' " + mapFlag + "-vf \"select='isnan(prev_selected_t)+gte(t-floor(prev_selected_t),%d)',scale=-2:%d,setpts=N/%f/TB\" -vsync vfr -q:v %d thumbnail-%%06d.jpg",
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
          .filter(p -> p.getFileName().toString().startsWith("thumbnail-"))
          .count();
    }
  }

  private static void generateSprites(Path dir, int spriteC, int spriteR, int spriteS, ThumbnailConfig config) throws Exception {

    List<Path> thumbnails;
    try (Stream<Path> stream = Files.list(dir)) {
      thumbnails = stream
          .filter(p -> p.getFileName().toString().startsWith("thumbnail-"))
          .sorted()
          .toList();
    }

    int spriteCount = (thumbnails.size() + spriteS - 1) / spriteS;

    // Step 1: tile thumbnails into sprite-N.jpg using +append (rows) then -append (columns)
    for (int s = 0; s < spriteCount; s++) {
      List<Path> batch = thumbnails.subList(s * spriteS, Math.min((s + 1) * spriteS, thumbnails.size()));
      List<Path> rowFiles = new ArrayList<>();

      for (int r = 0; r < spriteR; r++) {
        int from = r * spriteC;
        if (from >= batch.size()) break;
        List<Path> rowThumbs = batch.subList(from, Math.min(from + spriteC, batch.size()));

        Path rowFile = dir.resolve("row-" + s + "-" + r + ".jpg");
        List<String> cmd = new ArrayList<>(List.of("magick"));
        rowThumbs.forEach(p -> cmd.add(p.toString()));
        cmd.add("+append");
        cmd.add(rowFile.toString());
        runChecked(cmd, dir, "magick +append");
        rowFiles.add(rowFile);
      }

      Path spriteFile = dir.resolve("sprite-" + s + ".jpg");
      if (rowFiles.size() == 1) {
        Files.move(rowFiles.get(0), spriteFile, StandardCopyOption.REPLACE_EXISTING);
      } else {
        List<String> cmd = new ArrayList<>(List.of("magick"));
        rowFiles.forEach(p -> cmd.add(p.toString()));
        cmd.add("-append");
        cmd.add(spriteFile.toString());
        runChecked(cmd, dir, "magick -append");
        for (Path row : rowFiles) Files.deleteIfExists(row);
      }
    }

    // Step 2: convert each sprite-N.jpg to the target format
    StringBuilder convertFlags = new StringBuilder();
    convertFlags.append(String.format("-quality %d ", config.quality()));
    if (config instanceof WebpThumbnailConfig webp) {
      convertFlags.append(String.format("-define webp:preset=%s ", webp.preset()));
      if (webp.lossless()) convertFlags.append("-define webp:lossless=true ");
      convertFlags.append(String.format("-define webp:method=%d ", webp.method()));
    } else if (config instanceof AvifThumbnailConfig avif) {
      convertFlags.append(String.format("-define avif:speed=%d ", avif.speed()));
    }

    if (!config.format().equals("jpg")) {
      String convertCmd = String.format(
          "for f in sprite-*.jpg; do magick \"$f\" %s\"${f%%.jpg}.%s\" && rm \"$f\"; done",
          convertFlags, config.format());
      runChecked(convertCmd, dir, "magick convert");
    } else if (convertFlags.toString().isBlank()) {
      // jpg with default quality — already done, no conversion needed
    } else {
      String convertCmd = String.format(
          "for f in sprite-*.jpg; do magick \"$f\" %s\"$f\"; done", convertFlags);
      runChecked(convertCmd, dir, "magick convert");
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

    Files.writeString(vttFile, vtt.toString(), StandardOpenOption.CREATE_NEW);
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
