package com.rsargsyan.sprite.main_ctx;

import com.rsargsyan.sprite.main_ctx.core.ports.repository.ThumbnailsGenerationJobRepository;
import io.hypersistence.tsid.TSID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Instant;

@Slf4j
@Configuration
public class Config {

  @Autowired
  private ThumbnailsGenerationJobRepository thumbnailsGenerationJobRepository;
  @Value("${rabbitmq.queue}")
  public String queueName;

  @Value("${rabbitmq.topic.exchange.name}")
  public String topicExchangeName;

  @Value("${rabbitmq.routing-key}")
  public String routingKey;

  @Value("${s3.access-key-id}")
  public String s3AccessKeyId;

  @Value("${s3.secret-access-key}")
  public String s3SecretAccessKey;

  @Value("${s3.region}")
  public String s3Region;

  @Value("${s3.endpoint}")
  public String s3Endpoint;

  @Value("${s3.bucket}")
  public String s3Bucket;

  @Value("${job.max-video-file-size-bytes}")
  public long maxVideoFileSizeBytes;

  @Value("${job.min-free-disk-space-bytes}")
  public long minFreeDiskSpaceBytes;

  @Value("${ffmpeg.threads:2}")
  public int ffmpegThreads;

  @Value("${job.processing-pool-size:2}")
  public int processingPoolSize;

  @Value("${job.heartbeat-interval-seconds:30}")
  public int heartbeatIntervalSeconds;

  @Value("${job.stale-heartbeat-seconds:120}")
  public int staleHeartbeatSeconds;

  @Value("${job.max-retries:3}")
  public int maxRetries;

  @Value("${job.base-output-folder}")
  public String baseOutputFolder;

  @Value("${job.s3-expiry-seconds:604800}")
  public long s3ExpirySeconds;

  @Value("${job.retention-seconds:2592000}")
  public long retentionSeconds;

  @Value("${job.s3-expiry-safety-buffer-seconds:3600}")
  public long s3ExpirySafetyBufferSeconds;

  @Value("${job.presigned-url-max-seconds:86400}")
  public long presignedUrlMaxSeconds;

  @Bean
  public Queue queue() {
    return new Queue(queueName, true);
  }

  @Bean
  public TopicExchange exchange() {
    return new TopicExchange(topicExchangeName);
  }

  @Bean
  public Binding binding(Queue queue, TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with(routingKey);
  }

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(s3Endpoint))
        .region(Region.of(s3Region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  @SuppressWarnings(value = "unused")
  @Bean
  public RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
    connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setConfirmCallback((correlationData, ack, cause) -> {
      if (ack) {
        thumbnailsGenerationJobRepository.updateMqConfirmedAt(
            TSID.from(correlationData.getId()).toLong(), Instant.now());
      } else {
        log.warn("RabbitMQ NACKed message for job {}: {}", correlationData.getId(), cause);
      }
    });
    return template;
  }

  @SuppressWarnings(value = "unused")
  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .endpointOverride(URI.create(s3Endpoint))
        .region(Region.of(s3Region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey)))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

}
