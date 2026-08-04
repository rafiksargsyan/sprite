package com.rsargsyan.sprite.main_ctx.adapters.driving.workers;

import com.rabbitmq.client.Channel;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import com.rsargsyan.sprite.main_ctx.core.exception.DomainException;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Profile("worker")
@Slf4j
@Configuration("workersConfig")
@SuppressWarnings(value="unused")
public class Config {

  @SuppressWarnings(value="unused")
  @Autowired
  private com.rsargsyan.sprite.main_ctx.Config config;

  @SuppressWarnings(value="unused")
  @Autowired
  private ThumbnailsGenerationJobService thumbnailsGenerationJobService;

  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Bean
  SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                           MyMessageListener listener) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(config.queueName);
    container.setMessageListener(listener);
    container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    container.setPrefetchCount(5);
    return container;
  }

  @Component
  public class MyMessageListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) {
      try {
        thumbnailsGenerationJobService.receive(new String(message.getBody(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new IllegalStateException("This should never happen", e);
      } catch (ResourceNotFoundException e) {
        // Propagate: let RabbitMQ requeue and retry, in case the job's DB transaction
        // hasn't committed yet (publish-before-commit race in ApplicationEventListener).
        throw e;
      } catch (DomainException e) {
        log.warn("Received a domain exception", e);
        ack(channel, message);
        return;
      }
      // TODO: Maybe we can have multiple queues for specific duration ranges
      executorService.schedule(() -> ack(channel, message), 5, TimeUnit.MINUTES);
    }
  }

  private static void ack(Channel channel, Message message) {
    try {
      channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (IOException e) {
      log.warn("Failed to ACK message, RabbitMQ will redeliver", e);
    }
  }

}
