package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.VideoFileTooLargeException;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ApplicationEventListener {

  private static final String BASE_OUTPUT_FOLDER = "/Users/rsargsyan/Workspace/javacv-video";

  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final RabbitTemplate rabbitTemplate;
  private final S3TransferManager s3TransferManager;
  private final Config config;

  @Autowired
  public ApplicationEventListener(ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
                                  RabbitTemplate rabbitTemplate,
                                  S3TransferManager s3TransferManager,
                                  Config config) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.s3TransferManager = s3TransferManager;
    this.config = config;
  }

  @Async
  @TransactionalEventListener
  public void handleThumbnailsGenerationJobUpsertEvent(ThumbnailsGenerationJobUpsertEvent event) {
    thumbnailsGenerationJobRepository.findById(event.jobId()).ifPresentOrElse(
        job -> {
          if (job.getStatus() == ThumbnailsGenerationJob.Status.SUBMITTED) {
            job.queue();
            thumbnailsGenerationJobRepository.save(job);
            rabbitTemplate.convertAndSend(config.topicExchangeName, "test", job.getStrId());
          } else if (job.getStatus() == ThumbnailsGenerationJob.Status.IN_PROGRESS) {
            processVideoAndUpload(job);
          }
        },
        () -> {
          //TODO: log warning that job with the given id was not found
        }
    );
  }

  private void processVideoAndUpload(ThumbnailsGenerationJob job) {
    String jobId = job.getStrId();
    Path jobFolder = Paths.get(BASE_OUTPUT_FOLDER).resolve(jobId);
    Path zipFile = Paths.get(BASE_OUTPUT_FOLDER).resolve(jobId + ".zip");

    try {
      List<ThumbnailConfig> configs = job.getJobSpec().configs();
      String videoUrl = job.getVideoURL().toString();

      checkFileSize(videoUrl);

      String videoPath;
      if (configs.size() > 1 && hasSufficientDiskSpace()) {
        Files.createDirectories(jobFolder);
        videoPath = downloadVideo(videoUrl, jobFolder).toString();
      } else {
        videoPath = videoUrl;
      }

      for (ThumbnailConfig cfg : configs) {
        Path configFolder = jobFolder.resolve(cfg.folderName());
        VideoThumbnailGenerator.run(videoPath, configFolder, cfg, job.getStreamIndex());
      }

      zipDirectory(jobFolder, zipFile);

      if (job.isPreview()) {
        try {
          s3TransferManager.uploadDirectory(UploadDirectoryRequest.builder()
              .source(jobFolder)
              .bucket(config.s3Bucket)
              .s3Prefix(jobId)
              .build()
          ).completionFuture().join();
        } catch (Exception ignored) {
          // Directory upload is best-effort; zip upload is the source of truth
        }
      }

      s3TransferManager.uploadFile(UploadFileRequest.builder()
          .source(zipFile)
          .putObjectRequest(r -> r.bucket(config.s3Bucket).key(jobId + ".zip"))
          .build()
      ).completionFuture().join();

      job.succeed();
      thumbnailsGenerationJobRepository.save(job);

    } catch (Exception e) {
      job.fail(e.getMessage());
      thumbnailsGenerationJobRepository.save(job);
    } finally {
      deleteRecursively(jobFolder);
      try { Files.deleteIfExists(zipFile); } catch (IOException ignored) {}
    }
  }

  private void checkFileSize(String videoUrl) {
    try {
      var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      var request = HttpRequest.newBuilder()
          .method("HEAD", HttpRequest.BodyPublishers.noBody())
          .uri(URI.create(videoUrl))
          .build();
      var response = client.send(request, HttpResponse.BodyHandlers.discarding());
      response.headers().firstValueAsLong("content-length").ifPresent(size -> {
        if (size > config.maxVideoFileSizeBytes) {
          throw new VideoFileTooLargeException(size, config.maxVideoFileSizeBytes);
        }
      });
    } catch (VideoFileTooLargeException e) {
      throw e;
    } catch (Exception ignored) {
      // Can't determine size — proceed and let processing fail naturally if needed
    }
  }

  private boolean hasSufficientDiskSpace() {
    try {
      long freeSpace = Files.getFileStore(Paths.get(BASE_OUTPUT_FOLDER)).getUsableSpace();
      return freeSpace >= config.minFreeDiskSpaceBytes;
    } catch (IOException e) {
      return false;
    }
  }

  private static Path downloadVideo(String videoUrl, Path jobFolder) throws Exception {
    Path videoFile = jobFolder.resolve("video");
    var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var request = HttpRequest.newBuilder().uri(URI.create(videoUrl)).build();
    client.send(request, HttpResponse.BodyHandlers.ofFile(videoFile));
    return videoFile;
  }

  private static void zipDirectory(Path source, Path target) throws IOException {
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(target))) {
      Files.walkFileTree(source, new SimpleFileVisitor<>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          zos.putNextEntry(new ZipEntry(source.relativize(file).toString()));
          Files.copy(file, zos);
          zos.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

  private static void deleteRecursively(Path path) {
    if (!Files.exists(path)) return;
    try (var stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try { Files.delete(p); } catch (IOException ignored) {}
          });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
