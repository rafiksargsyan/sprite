package com.rsargsyan.sprite.main_ctx.adapters.driving.workers;

import com.rabbitmq.client.Channel;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import com.rsargsyan.sprite.main_ctx.core.exception.DomainException;
import com.rsargsyan.sprite.main_ctx.core.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration("workersConfig")
public class Config {

  @Autowired
  private com.rsargsyan.sprite.main_ctx.Config config;

  @Autowired
  private ThumbnailsGenerationJobService thumbnailsGenerationJobService;


  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

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
        log.warn("Job not found", e);
        ack(channel, message);
        return;
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

  @Bean
  Queue queue() {
    return new Queue(config.queueName, true);
  }

  @Bean
  TopicExchange exchange() {
    return new TopicExchange(config.topicExchangeName);
  }

  @Bean
  Binding binding(Queue queue, TopicExchange exchange) {
    return BindingBuilder.bind(queue).to(exchange).with("test");
  }
}
