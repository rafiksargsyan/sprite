package com.rsargsyan.sprite.main_ctx.core.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.util.stream.Stream;

public class VideoThumbnailGenerator {

  public static void run(String videoFilePath,
                         String outputFolder,
                         String resolutionsBase64Encoded) throws Exception {

    List<Integer> resolutions = decodeResolutions(resolutionsBase64Encoded);

    Path videoFileAbsolutePath = Paths.get(videoFilePath).toAbsolutePath();
    Path outputFolderAbsolutePath = Paths.get(outputFolder).toAbsolutePath();

    double fps = resolveFps(videoFileAbsolutePath);

    for (Integer res : resolutions) {

      int[] spriteSize = resolveSpriteSize(res);
      int spriteR = spriteSize[0];
      int spriteC = spriteSize[1];
      int spriteS = spriteR * spriteC;

      Path resAbsolutePath = outputFolderAbsolutePath.resolve(String.valueOf(res));
      Files.createDirectories(resAbsolutePath);

      generateThumbnails(videoFileAbsolutePath, resAbsolutePath, res, fps);

      Dimension dim = resolveImageSize(resAbsolutePath.resolve("thumbnail-000001.png"));

      int width = dim.width;
      int height = dim.height;

      int thumbnailsCount = countThumbnails(resAbsolutePath);

      generateSprites(resAbsolutePath, spriteC, spriteR);

      deleteThumbnails(resAbsolutePath);

      if (thumbnailsCount <= spriteS) {
        Files.move(
            resAbsolutePath.resolve("sprite.jpg"),
            resAbsolutePath.resolve("sprite-0.jpg"),
            StandardCopyOption.REPLACE_EXISTING
        );
      }

      generateWebVtt(
          resAbsolutePath,
          thumbnailsCount,
          spriteS,
          spriteC,
          width,
          height
      );
    }
  }

  private static List<Integer> decodeResolutions(String base64) throws Exception {

    byte[] decoded = Base64.getDecoder().decode(base64);
    String json = new String(decoded, StandardCharsets.UTF_8);

    ObjectMapper mapper = new ObjectMapper();
    Integer[] arr = mapper.readValue(json, Integer[].class);

    return Arrays.asList(arr);
  }

  private static double resolveFps(Path videoPath) throws Exception {

    ProcessBuilder pb = new ProcessBuilder(
        "ffprobe",
        "-v", "quiet",
        "-print_format", "json",
        "-show_streams",
        "-i", videoPath.toString()
    );

    Process process = pb.start();
    String json = new String(process.getInputStream().readAllBytes());

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(json);

    String rate = root.get("streams").get(1).get("r_frame_rate").asText(); // detect stream number

    String[] parts = rate.split("/");
    return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
  }

  private static void generateThumbnails(Path videoPath,
                                         Path outputDir,
                                         int resolution,
                                         double fps) throws Exception {

    String cmd = String.format(
        "ffmpeg -i %s -vf \"select='isnan(prev_selected_t)+gte(t-floor(prev_selected_t),1)',scale=-2:%d,setpts=N/%f/TB\" thumbnail-%%06d.png",
        videoPath,
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

  private static void generateSprites(Path dir, int spriteC, int spriteR) throws Exception {

    String cmd = String.format(
        "magick montage -quality 18 -geometry +0+0 -tile %dx%d thumbnail-*.png sprite.jpg",
        spriteC,
        spriteR
    );

    ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
    pb.directory(dir.toFile());

    Process p = pb.start();
    p.waitFor();
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
                                     int height) throws Exception {

    Path vttFile = dir.resolve("thumbnails.vtt");

    StringBuilder vtt = new StringBuilder("WEBVTT\n\n");

    for (int i = 0; i < thumbnailsCount; i++) {

      int spriteNumber = i / spriteS;

      int start = i;
      int end = i + 1;

      int spritePositionX = (i % spriteC) * width;
      int spritePositionY = ((i % spriteS) / spriteC) * height;

      vtt.append(vttTimestamp(start))
          .append(" --> ")
          .append(vttTimestamp(end))
          .append("\n");

      vtt.append(String.format(
          "sprite-%d.jpg#xywh=%d,%d,%d,%d\n\n",
          spriteNumber,
          spritePositionX,
          spritePositionY,
          width,
          height
      ));
    }

    Files.writeString(vttFile, vtt.toString(),
        StandardOpenOption.CREATE_NEW);
  }

  private static String vttTimestamp(int seconds) {

    int ms = seconds * 1000;

    int s = (ms / 1000) % 60;
    int m = (ms / 60000) % 60;
    int h = ms / 3600000;
    int milli = ms % 1000;

    return String.format("%02d:%02d:%02d.%03d", h, m, s, milli);
  }

  private static int[] resolveSpriteSize(int resolution) {

    if (resolution <= 60) return new int[]{8, 8};
    if (resolution <= 120) return new int[]{5, 5};
    return new int[]{3, 3};
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

