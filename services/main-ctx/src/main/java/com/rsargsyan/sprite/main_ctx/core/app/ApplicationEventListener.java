package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Slf4j
@Component
public class ApplicationEventListener {

  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ThumbnailsGenerationJobService thumbnailsGenerationJobService;
  private final Config config;
  private final ExecutorService processingExecutor;
  private final Semaphore processingSemaphore;

  @Autowired
  public ApplicationEventListener(ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
                                  RabbitTemplate rabbitTemplate,
                                  ThumbnailsGenerationJobService thumbnailsGenerationJobService,
                                  Config config) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.thumbnailsGenerationJobService = thumbnailsGenerationJobService;
    this.config = config;
    this.processingExecutor = Executors.newVirtualThreadPerTaskExecutor();
    this.processingSemaphore = new Semaphore(config.processingPoolSize);
  }

  @EventListener
  public void handleThumbnailsGenerationJobCreatedEvent(ThumbnailsGenerationJobCreatedEvent event) {
    thumbnailsGenerationJobRepository.findById(event.jobId()).ifPresentOrElse(
        job -> {
          if (job.getStatus() == ThumbnailsGenerationJob.Status.SUBMITTED) {
            job.queue();
            job.markMqSent();
            thumbnailsGenerationJobRepository.save(job);
            sendToRabbitMq(job.getStrId());
          }
        },
        () -> log.warn("Job not found for create event: {}", event.jobId())
    );
  }

  @EventListener
  public void handleThumbnailsGenerationJobRetryEvent(ThumbnailsGenerationJobRetryEvent event) {
    thumbnailsGenerationJobRepository.findById(event.jobId()).ifPresentOrElse(
        job -> {
          if (job.getStatus() == ThumbnailsGenerationJob.Status.RETRYING) {
            job.queue();
            job.markMqSent();
            thumbnailsGenerationJobRepository.save(job);
            sendToRabbitMq(job.getStrId());
          }
        },
        () -> log.warn("Job not found for retry event: {}", event.jobId())
    );
  }

  private void sendToRabbitMq(String strId) {
    rabbitTemplate.convertAndSend(config.topicExchangeName, "test", strId, m -> {
      m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
      return m;
    }, new CorrelationData(strId));
  }

  @Async
  @TransactionalEventListener
  public void handleThumbnailsGenerationJobReceivedEvent(ThumbnailsGenerationJobReceivedEvent event) {
    thumbnailsGenerationJobService.startHeartbeat(event.jobId());
    processingExecutor.submit(() -> {
      try {
        processingSemaphore.acquire();
        try {
          thumbnailsGenerationJobService.process(event.jobId());
        } finally {
          processingSemaphore.release();
        }
      } catch (Exception e) {
        log.error("Unexpected error processing job {}", event.jobId(), e);
      }
    });
  }
}
