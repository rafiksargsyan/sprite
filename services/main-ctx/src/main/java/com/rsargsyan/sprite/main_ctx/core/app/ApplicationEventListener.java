package com.rsargsyan.sprite.main_ctx.core.app;

import com.nimbusds.jose.util.Base64;
import com.rsargsyan.sprite.main_ctx.Config;
import com.rsargsyan.sprite.main_ctx.core.domain.aggregate.ThumbnailsGenerationJob;
import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

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
    String resolutions = "[120]";
    VideoThumbnailGenerator.run("/Users/rsargsyan/Workspace/tmp/BigBuckBunny.mp4",
        "/Users/rsargsyan/Workspace/javacv-video",
        Base64.encode(resolutions.getBytes()).toString());
  }
}
