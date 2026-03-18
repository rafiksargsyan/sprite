package com.rsargsyan.sprite.main_ctx.core.app;

import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class ApplicationEventListener {

  private final ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  private final RabbitTemplate rabbitTemplate;
  private final ThumbnailsGenerationJobService thumbnailsGenerationJobService;
  private final Config config;
  private final ExecutorService processingExecutor;

  @Autowired
  public ApplicationEventListener(ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository,
                                  RabbitTemplate rabbitTemplate,
                                  ThumbnailsGenerationJobService thumbnailsGenerationJobService,
                                  Config config) {
    this.thumbnailsGenerationJobRepository = thumbnailsGenerationJobRepository;
    this.rabbitTemplate = rabbitTemplate;
    this.thumbnailsGenerationJobService = thumbnailsGenerationJobService;
    this.config = config;
    this.processingExecutor = Executors.newFixedThreadPool(config.processingPoolSize);
  }

  @Async
  @TransactionalEventListener
  public void handleThumbnailsGenerationJobCreatedEvent(ThumbnailsGenerationJobCreatedEvent event) {
    thumbnailsGenerationJobRepository.findById(event.jobId()).ifPresentOrElse(
        job -> {
          if (job.getStatus() == ThumbnailsGenerationJob.Status.SUBMITTED) {
            job.queue();
            thumbnailsGenerationJobRepository.save(job);
            rabbitTemplate.convertAndSend(config.topicExchangeName, "test", job.getStrId(), m -> {
              m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
              return m;
            });
          }
        },
        () -> log.warn("Job not found for create event: {}", event.jobId())
    );
  }

  @Async
  @TransactionalEventListener
  public void handleThumbnailsGenerationJobReceivedEvent(ThumbnailsGenerationJobReceivedEvent event) {
    processingExecutor.submit(() -> {
      try {
        thumbnailsGenerationJobService.process(event.jobId());
      } catch (Exception e) {
        log.error("Unexpected error processing job {}", event.jobId(), e);
      }
    });
  }
}
