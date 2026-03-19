package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FailureReason;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.*;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.JobSpecRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ThumbnailsGenerationJobService {

  private static final Duration DOWNLOAD_WINDOW = Duration.ofHours(2);

  private static final Duration PREVIEW_URL_TTL = Duration.ofHours(1);

  private final ApplicationEventPublisher applicationEventPublisher;
  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final AccountRepository accountRepository;
  private final JobSpecRepository jobSpecRepository;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final Config config;
  private final TransactionTemplate transactionTemplate;
  private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
  private final ConcurrentHashMap<Long, ScheduledFuture<?>> activeHeartbeats = new ConcurrentHashMap<>();

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher,
      AccountRepository accountRepository,
      JobSpecRepository jobSpecRepository,
      S3Client s3Client,
      S3Presigner s3Presigner,
      Config config,
      TransactionTemplate transactionTemplate
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accountRepository = accountRepository;
    this.jobSpecRepository = jobSpecRepository;
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.config = config;
    this.transactionTemplate = transactionTemplate;
  }

  public Page<ThumbnailsGenerationJobDTO> findAll(String accountId, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return thumbnailsGenerationJobRepository.findByAccountId(Util.validateTSID(accountId), pageable)
        .map(job -> ThumbnailsGenerationJobDTO.from(job, presignedDownloadUrl(job), isPreviewAvailable(job)));
  }

  public ThumbnailsGenerationJobDTO findById(String accountId, String jobId) {
    var job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobId))
        .orElseThrow(ResourceNotFoundException::new);
    return ThumbnailsGenerationJobDTO.from(job, presignedDownloadUrl(job), isPreviewAvailable(job));
  }

  public Map<String, String> getPreviewFiles(String accountId, String jobId, String configFolderName) {
    var job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobId))
        .orElseThrow(ResourceNotFoundException::new);

    if (!job.isPreview() || job.getStatus() != ThumbnailsGenerationJob.Status.SUCCESS) {
      throw new ResourceNotFoundException();
    }

    String prefix = job.getStrId() + "/" + configFolderName + "/";
    var listed = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(config.s3Bucket)
        .prefix(prefix)
        .build());

    Map<String, String> result = new LinkedHashMap<>();
    for (var obj : listed.contents()) {
      String filename = obj.key().substring(prefix.length());
      if (filename.isEmpty()) continue;
      var presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(PREVIEW_URL_TTL)
          .getObjectRequest(GetObjectRequest.builder()
              .bucket(config.s3Bucket)
              .key(obj.key())
              .build())
          .build();
      result.put(filename, s3Presigner.presignGetObject(presignRequest).url().toString());
    }
    return result;
  }

  public String getPreviewVtt(String accountId, String jobId, String configFolderName) {
    var job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobId))
        .orElseThrow(ResourceNotFoundException::new);

    if (!job.isPreview() || job.getStatus() != ThumbnailsGenerationJob.Status.SUCCESS) {
      throw new ResourceNotFoundException();
    }

    String key = job.getStrId() + "/" + configFolderName + "/thumbnails.vtt";
    try {
      return s3Client.getObjectAsBytes(GetObjectRequest.builder()
              .bucket(config.s3Bucket)
              .key(key)
              .build())
          .asUtf8String();
    } catch (NoSuchKeyException e) {
      throw new ResourceNotFoundException();
    }
  }

  public long getMaxFileSizeBytes() {
    return config.maxVideoFileSizeBytes;
  }

  @Transactional
  public ThumbnailsGenerationJobDTO create(String accountId, ThumbnailsGenerationJobCreationDTO dto) {
    var accountLongId = Util.validateTSID(accountId);
    var account = accountRepository.findById(accountLongId)
        .orElseThrow(ResourceNotFoundException::new);
    var jobSpec = jobSpecRepository.findByAccountIdAndId(accountLongId, Util.validateTSID(dto.getJobSpecId()))
        .orElseThrow(ResourceNotFoundException::new);
    var job = new ThumbnailsGenerationJob(account, dto.getVideoURL(), jobSpec.toEmbedded(), dto.getStreamIndex(), dto.isPreview());
    thumbnailsGenerationJobRepository.save(job);
    applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobCreatedEvent(job.getId()));
    return ThumbnailsGenerationJobDTO.from(job, null, false);
  }

  @Transactional
  public void receive(String id) {
    thumbnailsGenerationJobRepository.findById(Util.validateTSID(id)).ifPresentOrElse(
        job -> {
          ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
              () -> thumbnailsGenerationJobRepository.updateHeartbeat(job.getId(), Instant.now()),
              0,
              config.heartbeatIntervalSeconds,
              TimeUnit.SECONDS
          );
          activeHeartbeats.put(job.getId(), heartbeat);
          job.receive();
          thumbnailsGenerationJobRepository.save(job);
          applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobReceivedEvent(job.getId()));
        },
        () -> { throw new ResourceNotFoundException(); }
    );
  }

  void process(Long jobId) {
    ScheduledFuture<?> heartbeat = activeHeartbeats.get(jobId);

    var job = transactionTemplate.execute(status -> {
      var j = thumbnailsGenerationJobRepository.findById(jobId)
          .orElseThrow(ResourceNotFoundException::new);
      j.run();
      thumbnailsGenerationJobRepository.save(j);
      return j;
    });
    if (job == null) return;

    String strId = job.getStrId();
    Path jobFolder = Paths.get(config.baseOutputFolder).resolve(strId);
    Path zipFile = Paths.get(config.baseOutputFolder).resolve(strId + ".zip");

    try {
      List<ThumbnailConfig> configs = job.getJobSpec().configs();
      String videoUrl = job.getVideoURL().toString();

      checkVideoAccessibility(videoUrl);

      String videoPath;
      if (configs.size() > 1 && hasSufficientDiskSpace()) {
        Files.createDirectories(jobFolder);
        long t = System.nanoTime();
        videoPath = downloadVideo(videoUrl, jobFolder).toString();
        log.info("[{}] Download: {}s", strId, elapsed(t));
      } else {
        videoPath = videoUrl;
      }

      for (ThumbnailConfig cfg : configs) {
        Path configFolder = jobFolder.resolve(cfg.folderName());
        long t = System.nanoTime();
        try {
          VideoThumbnailGenerator.run(videoPath, configFolder, cfg, job.getStreamIndex(), config.ffmpegThreads);
        } catch (InvalidThumbnailConfigException e) {
          throw e;
        } catch (Exception e) {
          throw new JobFailureException(FailureReason.PROCESSING_FAILED, e);
        }
        log.info("[{}] Transcoding ({}): {}s", strId, cfg.folderName(), elapsed(t));
      }

      long t = System.nanoTime();
      zipDirectory(jobFolder, zipFile);
      log.info("[{}] Zip: {}s", strId, elapsed(t));

      if (job.isPreview()) {
        try {
          long pt = System.nanoTime();
          awsS3Upload(jobFolder, strId + "/", true);
          log.info("[{}] Preview upload: {}s", strId, elapsed(pt));
        } catch (Exception ignored) {
          // Directory upload is best-effort; zip upload is the source of truth
        }
      }

      long ut = System.nanoTime();
      try {
        awsS3Upload(zipFile, strId + ".zip", false);
      } catch (Exception e) {
        throw new JobFailureException(FailureReason.UPLOAD_FAILED, e);
      }
      log.info("[{}] ZIP upload: {}s", strId, elapsed(ut));

      transactionTemplate.executeWithoutResult(status -> {
        var j = thumbnailsGenerationJobRepository.findById(jobId).orElseThrow(ResourceNotFoundException::new);
        j.succeed();
        thumbnailsGenerationJobRepository.save(j);
      });

    } catch (Exception e) {
      log.error("[{}] Processing failed: {}", strId, e.getMessage(), e);
      FailureReason reason = toFailureReason(e);
      try {
        transactionTemplate.executeWithoutResult(status -> {
          var j = thumbnailsGenerationJobRepository.findById(jobId).orElseThrow(ResourceNotFoundException::new);
          j.fail(reason);
          thumbnailsGenerationJobRepository.save(j);
        });
      } catch (Exception saveException) {
        log.error("[{}] Failed to persist failure status", strId, saveException);
      }
    } finally {
      activeHeartbeats.remove(jobId);
      if (heartbeat != null) heartbeat.cancel(false);
      deleteRecursively(jobFolder);
      try { Files.deleteIfExists(zipFile); } catch (IOException ignored) {}
    }
  }

  private void checkVideoAccessibility(String videoUrl) {
    try {
      var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
      var request = HttpRequest.newBuilder()
          .method("HEAD", HttpRequest.BodyPublishers.noBody())
          .uri(URI.create(videoUrl))
          .build();
      var response = client.send(request, HttpResponse.BodyHandlers.discarding());
      int status = response.statusCode();
      if (status >= 400 && status != 405) {
        throw new VideoNotAccessibleException("Video returned HTTP " + status + ": " + videoUrl);
      }
      response.headers().firstValueAsLong("content-length").ifPresent(size -> {
        if (size > config.maxVideoFileSizeBytes) {
          throw new VideoFileTooLargeException(size, config.maxVideoFileSizeBytes);
        }
      });
    } catch (VideoNotAccessibleException | VideoFileTooLargeException e) {
      throw e;
    } catch (Exception ignored) {
      // Can't reach server — proceed and let ffprobe surface the real error
    }
  }

  private boolean hasSufficientDiskSpace() {
    try {
      long freeSpace = Files.getFileStore(Paths.get(config.baseOutputFolder)).getUsableSpace();
      return freeSpace >= config.minFreeDiskSpaceBytes;
    } catch (IOException e) {
      return false;
    }
  }

  private static Path downloadVideo(String videoUrl, Path jobFolder) {
    Path videoFile = jobFolder.resolve("video");
    var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    var request = HttpRequest.newBuilder().uri(URI.create(videoUrl)).build();
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofFile(videoFile));
      if (response.statusCode() >= 400) {
        throw new VideoNotAccessibleException("Video returned HTTP " + response.statusCode() + ": " + videoUrl);
      }
    } catch (VideoNotAccessibleException e) {
      throw e;
    } catch (Exception e) {
      throw new VideoNotAccessibleException("Failed to download video: " + videoUrl, e);
    }
    return videoFile;
  }

  private void awsS3Upload(Path source, String s3Key, boolean recursive) throws Exception {
    ProcessBuilder pb = new ProcessBuilder(
        "aws", "s3", "cp",
        source.toString(),
        "s3://" + config.s3Bucket + "/" + s3Key,
        "--endpoint-url", config.s3Endpoint
    );
    if (recursive) {
      pb.command().add("--recursive");
    }
    pb.environment().put("AWS_ACCESS_KEY_ID", config.s3AccessKeyId);
    pb.environment().put("AWS_SECRET_ACCESS_KEY", config.s3SecretAccessKey);
    pb.environment().put("AWS_DEFAULT_REGION", config.s3Region);
    pb.inheritIO();
    int exitCode = pb.start().waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("aws s3 cp failed with exit code " + exitCode);
    }
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

  private static long elapsed(long startNano) {
    return TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNano);
  }

  private static FailureReason toFailureReason(Exception e) {
    if (e instanceof JobFailureException jfe) return jfe.reason;
    if (e instanceof VideoFileTooLargeException) return FailureReason.VIDEO_TOO_LARGE;
    if (e instanceof VideoNotAccessibleException) return FailureReason.VIDEO_NOT_ACCESSIBLE;
    if (e instanceof InvalidThumbnailConfigException) return FailureReason.INVALID_STREAM_INDEX;
    return FailureReason.UNKNOWN;
  }

  private boolean isPreviewAvailable(ThumbnailsGenerationJob job) {
    if (!job.isPreview() || job.getStatus() != ThumbnailsGenerationJob.Status.SUCCESS) return false;
    return Instant.now().isBefore(job.getFinishedAt().plus(DOWNLOAD_WINDOW));
  }

  private String presignedDownloadUrl(ThumbnailsGenerationJob job) {
    if (job.getStatus() != ThumbnailsGenerationJob.Status.SUCCESS) return null;
    Instant expiry = job.getFinishedAt().plus(DOWNLOAD_WINDOW);
    Instant now = Instant.now();
    if (!now.isBefore(expiry)) return null;
    var presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.between(now, expiry))
        .getObjectRequest(GetObjectRequest.builder()
            .bucket(config.s3Bucket)
            .key(job.getStrId() + ".zip")
            .build())
        .build();
    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
