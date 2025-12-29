package com.rsargsyan.sprite.main_ctx.adapters.driving.workers;

import com.rabbitmq.client.Channel;
import com.rsargsyan.sprite.main_ctx.core.app.ThumbnailsGenerationJobService;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class Config {

  @Autowired
  private com.rsargsyan.sprite.main_ctx.Config config;

  @Autowired
  private ThumbnailsGenerationJobService thumbnailsGenerationJobService;
  @Bean
  SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
                                           MyMessageListener listener) {
    SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.setQueueNames(config.queueName);
    container.setMessageListener(listener);
    container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    return container;
  }

  @Component
  public class MyMessageListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) {
      System.out.println("Received <" + message + ">");
      try {
        thumbnailsGenerationJobService.run("TODO");
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Bean
  Queue queue() {
    return new Queue(config.queueName, false);
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
