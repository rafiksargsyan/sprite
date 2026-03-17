package com.rsargsyan.sprite.main_ctx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

import java.net.URI;

@Configuration
public class Config {
  @Value("${rabbitmq.queue}")
  public String queueName;

  @Value("${rabbitmq.topic.exchange.name}")
  public String topicExchangeName;

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

  @Bean
  public S3Client s3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create(s3Endpoint))
        .region(Region.of(s3Region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey)))
        .build();
  }

  @Bean
  public S3AsyncClient s3AsyncClient() {
    return S3AsyncClient.builder()
        .endpointOverride(URI.create(s3Endpoint))
        .region(Region.of(s3Region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey)))
        .build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
        .endpointOverride(URI.create(s3Endpoint))
        .region(Region.of(s3Region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey)))
        .build();
  }

  @Bean(destroyMethod = "close")
  public S3TransferManager s3TransferManager(S3AsyncClient s3AsyncClient) {
    return S3TransferManager.builder()
        .s3Client(s3AsyncClient)
        .build();
  }
}
