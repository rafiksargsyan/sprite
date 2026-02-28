package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bytedeco.javacv.*;
import org.bytedeco.ffmpeg.global.avutil;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;

@Component
public class ApplicationEventListener {
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final RabbitTemplate rabbitTemplate;
  private final Config config;

  @Autowired
  public ApplicationEventListener(ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
                                  RabbitTemplate rabbitTemplate, Config config) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.config = config;
  }

  @Async
  @TransactionalEventListener
  public void handleThumbnailsGenerationJobUpsertEvent(ThumbnailsGenerationJobUpsertEvent event) {
    thumbnailsGenerationJobRepository.findById(event.getJobId()).ifPresentOrElse(
        job -> {
          if (job.getStatus() == ThumbnailsGenerationJob.Status.SUBMITTED) {
            job.queue();
            //TODO: send to RabbitMQ
            rabbitTemplate.convertAndSend(config.topicExchangeName, "test", job.getStrId());
            thumbnailsGenerationJobRepository.save(job);
          } else if (job.getStatus() == ThumbnailsGenerationJob.Status.IN_PROGRESS) {
            try {
              processVideoAndUpload(job.getVideoURL().toString());
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        () -> {
          //TODO: log warning message that job with the id was not found
        }
    );
  }

  private void processVideoAndUpload(String videoUrl) throws Exception {

    Path tempDir = Files.createDirectory(Path.of("/Users/rsargsyan/Workspace/javacv-video"));

    Path thumbsDir = tempDir.resolve("thumbs");
    Files.createDirectories(thumbsDir);

    // Allow unsafe network protocols if needed
    avutil.av_log_set_level(avutil.AV_LOG_ERROR);

    FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoUrl);
    grabber.start();

    double frameRate = grabber.getFrameRate();
    long totalFrames = grabber.getLengthInFrames();

    Java2DFrameConverter converter = new Java2DFrameConverter();

    int frameNumber = 0;
    int thumbIndex = 0;

    while (frameNumber < totalFrames) {

      grabber.setFrameNumber(frameNumber);
      Frame frame = grabber.grabImage();

      if (frame != null) {
        BufferedImage image = converter.convert(frame);

        if (image != null) {
          File output = thumbsDir.resolve(
              String.format("thumb_%04d.jpg", thumbIndex++)
          ).toFile();

          ImageIO.write(image, "jpg", output);
        }
      }

      frameNumber += (int) frameRate; // 1 second interval
    }

    grabber.stop();

    // Zip thumbnails
    Path zipPath = tempDir.resolve("thumbnails.zip");

    try (ZipOutputStream zos = new ZipOutputStream(
        new FileOutputStream(zipPath.toFile()))) {

      Files.list(thumbsDir).forEach(path -> {
        try {
          zos.putNextEntry(new ZipEntry(path.getFileName().toString()));
          Files.copy(path, zos);
          zos.closeEntry();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    S3Client s3 = config.s3Client();

    PutObjectRequest request = PutObjectRequest.builder()
        .bucket("sprite-rsargsyan")
        .key("thumbnails-zip.zip")
        .contentType("application/zip")
        .build();

    s3.putObject(request, RequestBody.fromFile(zipPath));

    // Cleanup
    Files.walk(tempDir)
        .sorted((a, b) -> b.compareTo(a))
        .forEach(p -> p.toFile().delete());
  }
}
