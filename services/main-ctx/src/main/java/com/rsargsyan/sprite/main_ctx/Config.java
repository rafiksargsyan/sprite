package com.rsargsyan.sprite.main_ctx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class Config {
  @Value("${rabbitmq.queue}")
  public String queueName;

  @Value("${rabbitmq.topic.exchange.name}")
  public String topicExchangeName;

  @Bean
  public S3Client s3Client() {

    AwsBasicCredentials credentials =
        AwsBasicCredentials.create(
            "todo",
            "todo"
        );

    return S3Client.builder()
        .endpointOverride(URI.create("https://s3.eu-central-003.backblazeb2.com")) // required for S3-compatible
        .region(Region.of("eu-central-003"))
        .credentialsProvider(
            StaticCredentialsProvider.create(credentials))
        .build();
  }
}
