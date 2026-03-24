package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.Util;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobCreationDTO;
import com.rsargsyan.sprite.main_ctx.core.app.dto.ThumbnailsGenerationJobDTO;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ConfigProcessingStats;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.FailureReason;
import com.rsargsyan.sprite.main_ctx.core.domain.valueobject.ThumbnailConfig;
import com.rsargsyan.sprite.main_ctx.core.exception.*;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.AccountRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.JobSpecRepository;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import io.hypersistence.tsid.TSID;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.nio.file.StandardCopyOption;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
  private final RabbitTemplate rabbitTemplate;
  private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
  private final ConcurrentHashMap<Long, ScheduledFuture<?>> activeHeartbeats = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Process> activeProcesses = new ConcurrentHashMap<>();
  private final ScheduledExecutorService cancellationChecker = Executors.newSingleThreadScheduledExecutor();
  private final ScheduledExecutorService outputFolderCleaner = Executors.newSingleThreadScheduledExecutor();

  @Autowired
  public ThumbnailsGenerationJobService(
      ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
      ApplicationEventPublisher applicationEventPublisher,
      AccountRepository accountRepository,
      JobSpecRepository jobSpecRepository,
      S3Client s3Client,
      S3Presigner s3Presigner,
      Config config,
      TransactionTemplate transactionTemplate,
      RabbitTemplate rabbitTemplate
  ) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.applicationEventPublisher = applicationEventPublisher;
    this.accountRepository = accountRepository;
    this.jobSpecRepository = jobSpecRepository;
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.config = config;
    this.transactionTemplate = transactionTemplate;
    this.rabbitTemplate = rabbitTemplate;
    this.cancellationChecker.scheduleAtFixedRate(this::checkCancellations, 3, 3, TimeUnit.SECONDS);
    this.outputFolderCleaner.scheduleAtFixedRate(this::cleanOutputFolder, 1, 1, TimeUnit.HOURS);
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
  public void cancel(String accountId, String jobId) {
    var job = thumbnailsGenerationJobRepository
        .findByAccountIdAndId(Util.validateTSID(accountId), Util.validateTSID(jobId))
        .orElseThrow(ResourceNotFoundException::new);
    job.cancel();
    thumbnailsGenerationJobRepository.save(job);
  }

  private void checkCancellations() {
    if (activeProcesses.isEmpty()) return;
    try {
      thumbnailsGenerationJobRepository.findAllById(activeProcesses.keySet()).forEach(job -> {
        if (job.getStatus() == ThumbnailsGenerationJob.Status.CANCELLED) {
          Process process = activeProcesses.get(job.getId());
          if (process != null) {
            log.info("[{}] Cancelling active process", job.getStrId());
            process.destroyForcibly();
          }
        }
      });
    } catch (Exception e) {
      log.warn("Error in cancellation checker: {}", e.getMessage());
    }
  }

  private void cleanOutputFolder() {
    Path root = Paths.get(config.baseOutputFolder);
    if (!Files.exists(root)) return;
    Instant cutoff = Instant.now().minus(Duration.ofDays(1));
    try (var stream = Files.list(root)) {
      stream.filter(Files::isDirectory).forEach(dir -> {
        try {
          if (Files.getLastModifiedTime(dir).toInstant().isBefore(cutoff)) {
            deleteRecursively(dir);
            log.info("Cleaned up stale output folder: {}", dir.getFileName());
          }
        } catch (Exception e) {
          log.warn("Failed to clean output folder {}: {}", dir.getFileName(), e.getMessage());
        }
      });
    } catch (Exception e) {
      log.warn("Output folder cleanup failed: {}", e.getMessage());
    }
  }

  @Transactional
  public void retryStuckJobs() {
    Instant threshold = Instant.now().minusSeconds(config.staleHeartbeatSeconds);
    List<ThumbnailsGenerationJob> stuckJobs = thumbnailsGenerationJobRepository.findStuckJobs(threshold);
    for (ThumbnailsGenerationJob job : stuckJobs) {
      if (job.getRetryCount() >= config.maxRetries) {
        log.warn("[{}] Job exceeded max retries ({}), marking as FAILURE", job.getStrId(), config.maxRetries);
        job.fail(FailureReason.UNKNOWN);
      } else {
        log.warn("[{}] Retrying stuck job (attempt {})", job.getStrId(), job.getRetryCount() + 1);
        Process stuckProcess = activeProcesses.remove(job.getId());
        if (stuckProcess != null) stuckProcess.destroyForcibly();
        job.retry();
        applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobRetryEvent(job.getId()));
      }
      thumbnailsGenerationJobRepository.save(job);
    }

    Instant mqConfirmThreshold = Instant.now().minusSeconds(30);
    for (ThumbnailsGenerationJob job : thumbnailsGenerationJobRepository.findStuckQueuedJobs(mqConfirmThreshold)) {
      log.warn("[{}] QUEUED job unconfirmed for >30s, resending to RabbitMQ", job.getStrId());
      try {
        thumbnailsGenerationJobRepository.updateMqSent(job.getId(), Instant.now());
        sendToRabbitMq(job.getStrId());
      } catch (Exception e) {
        log.warn("[{}] Failed to resend to RabbitMQ: {}", job.getStrId(), e.getMessage());
      }
    }
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
          job.receive();
          thumbnailsGenerationJobRepository.save(job);
          applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobReceivedEvent(job.getId()));
        },
        () -> { throw new ResourceNotFoundException(); }
    );
  }

  void startHeartbeat(Long jobId) {
    ScheduledFuture<?> heartbeat = heartbeatScheduler.scheduleAtFixedRate(
        () -> {
          try {
            thumbnailsGenerationJobRepository.updateHeartbeat(jobId, Instant.now());
          } catch (Exception e) {
            log.warn("[{}] Heartbeat update failed: {}", TSID.from(jobId), e.getMessage());
          }
        },
        0,
        config.heartbeatIntervalSeconds,
        TimeUnit.SECONDS
    );
    activeHeartbeats.put(jobId, heartbeat);
  }

  void process(Long jobId) {
    ScheduledFuture<?> heartbeat = activeHeartbeats.get(jobId);
    Path jobFolder = null;
    Path zipFile = null;
    Path videoFile = null;
    final String strId = TSID.from(jobId).toString();

    try {
      var job = transactionTemplate.execute(status -> {
        var j = thumbnailsGenerationJobRepository.findById(jobId)
            .orElseThrow(ResourceNotFoundException::new);
        j.run();
        thumbnailsGenerationJobRepository.save(j);
        return j;
      });
      if (job == null) return;

      String attemptKey = strId + "-" + job.getRetryCount();
      jobFolder = Paths.get(config.baseOutputFolder).resolve(attemptKey);
      zipFile = Paths.get(config.baseOutputFolder).resolve(attemptKey + ".zip");
      List<ThumbnailConfig> configs = job.getJobSpec().configs();
      String videoUrl = job.getVideoURL().toString();

      checkVideoAccessibility(videoUrl);

      long probeT = System.nanoTime();
      VideoProbeResult probe;
      try {
        probe = VideoThumbnailGenerator.probe(videoUrl, job.getStreamIndex());
      } catch (InvalidThumbnailConfigException e) {
        throw e;
      } catch (Exception e) {
        throw new JobFailureException(FailureReason.PROCESSING_FAILED, e);
      }
      log.info("[{}] Probe: {}s (codec={}, inputRes={}, duration={}s)", strId, elapsed(probeT),
          probe.codec(), probe.inputRes(), probe.durationSec());

      double extractionCost = ExtractionCostCalculator.calculate(
          probe.durationSec(), probe.codec(), probe.inputRes()) * configs.size();
      double postProcessingCost = configs.stream()
          .mapToDouble(cfg -> cfg.postProcessingCost(probe.durationSec() / cfg.interval(), cfg.resolution()))
          .sum();
      double totalCost = extractionCost + postProcessingCost;
      transactionTemplate.executeWithoutResult(s -> {
        ThumbnailsGenerationJob j = thumbnailsGenerationJobRepository.findById(job.getId()).orElseThrow();
        j.recordCost(totalCost);
        thumbnailsGenerationJobRepository.save(j);
      });
      log.info("[{}] Cost: {} (extraction={}, postProcessing={})", strId, totalCost, extractionCost, postProcessingCost);

      String videoPath;
      if (hasSufficientDiskSpace()) {
        Files.createDirectories(jobFolder);
        videoFile = jobFolder.getParent().resolve(attemptKey + ".video");
        log.info("[{}] Downloading video: {}", strId, videoUrl);
        long t = System.nanoTime();
        videoPath = downloadVideo(videoUrl, videoFile).toString();
        log.info("[{}] Download: {}s", strId, elapsed(t));
      } else {
        videoPath = videoUrl;
      }

      List<ConfigProcessingStats> statsList = new ArrayList<>();
      for (ThumbnailConfig cfg : configs) {
        Path configFolder = jobFolder.resolve(cfg.folderName());
        try {
          ConfigProcessingStats stats = VideoThumbnailGenerator.run(videoPath, configFolder, cfg, job.getStreamIndex(), config.ffmpegThreads, probe.fps(), p -> activeProcesses.put(jobId, p));
          statsList.add(stats);
          log.info("[{}] Transcoding ({}) extraction={}ms postProcessing={}ms",
              strId, cfg.folderName(), stats.extractionMs(), stats.postProcessingMs());
        } catch (InvalidThumbnailConfigException e) {
          throw e;
        } catch (Exception e) {
          throw new JobFailureException(FailureReason.PROCESSING_FAILED, e);
        }
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
        j.recordStats(statsList);
        j.succeed();
        thumbnailsGenerationJobRepository.save(j);
      });

    } catch (Exception e) {
      boolean cancelled = thumbnailsGenerationJobRepository.findById(jobId)
          .map(j -> j.getStatus() == ThumbnailsGenerationJob.Status.CANCELLED)
          .orElse(false);
      if (cancelled) {
        log.info("[{}] Job was cancelled", strId);
      } else {
        log.error("[{}] Processing failed: {}", strId, e.getMessage(), e);
        FailureReason reason = toFailureReason(e);
        boolean retryable = reason == FailureReason.UPLOAD_FAILED || reason == FailureReason.PROCESSING_FAILED;
        if (reason == FailureReason.VIDEO_NOT_ACCESSIBLE && !isNetworkReachable()) {
          log.warn("[{}] Video not accessible but network is down — will retry", strId);
          retryable = true;
        }
        try {
          final boolean doRetry = retryable;
          transactionTemplate.executeWithoutResult(status -> {
            var j = thumbnailsGenerationJobRepository.findById(jobId).orElseThrow(ResourceNotFoundException::new);
            if (doRetry && j.getRetryCount() < config.maxRetries) {
              log.warn("[{}] {} failed, scheduling retry (attempt {})", strId, reason, j.getRetryCount() + 1);
              j.retry();
              thumbnailsGenerationJobRepository.save(j);
              applicationEventPublisher.publishEvent(new ThumbnailsGenerationJobRetryEvent(jobId));
            } else {
              j.fail(reason);
              thumbnailsGenerationJobRepository.save(j);
            }
          });
        } catch (Exception saveException) {
          log.error("[{}] Failed to persist failure status", strId, saveException);
        }
      }
    } finally {
      activeHeartbeats.remove(jobId, heartbeat);
      activeProcesses.remove(jobId);
      if (heartbeat != null) heartbeat.cancel(false);
      if (jobFolder != null) deleteRecursively(jobFolder);
      try { if (zipFile != null) Files.deleteIfExists(zipFile); } catch (IOException ignored) {}
      try { if (videoFile != null) Files.deleteIfExists(videoFile); } catch (IOException ignored) {}
    }
  }

  private void sendToRabbitMq(String strId) {
    rabbitTemplate.convertAndSend(config.topicExchangeName, "test", strId, m -> {
      m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
      return m;
    }, new CorrelationData(strId));
  }

  private static boolean isNetworkReachable() {
    try (var socket = new java.net.Socket()) {
      socket.connect(new java.net.InetSocketAddress("8.8.8.8", 53), 3_000);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private void checkVideoAccessibility(String videoUrl) {
    try {
      var client = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(15))
          .build();
      var request = HttpRequest.newBuilder()
          .method("HEAD", HttpRequest.BodyPublishers.noBody())
          .uri(URI.create(videoUrl))
          .timeout(Duration.ofSeconds(30))
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

  private static Path downloadVideo(String videoUrl, Path videoFile) {
    try {
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(videoUrl).openConnection();
      conn.setConnectTimeout(15_000);
      conn.setReadTimeout(30_000);
      conn.setInstanceFollowRedirects(true);
      int status = conn.getResponseCode();
      if (status >= 400) {
        throw new VideoNotAccessibleException("Video returned HTTP " + status + ": " + videoUrl);
      }
      try (var in = conn.getInputStream()) {
        Files.copy(in, videoFile, StandardCopyOption.REPLACE_EXISTING);
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
