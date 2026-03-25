package com.sep490.g28.hvh.be.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqNotificationConfig {

    private static final String HEADER_MESSAGE_TTL = "x-message-ttl";
    private static final String HEADER_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    private static final String HEADER_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

    private final RabbitMqNotificationProperties properties;

    public RabbitMqNotificationConfig(RabbitMqNotificationProperties properties) {
        this.properties = properties;
    }

    @Bean
    DirectExchange notificationExchange() {
        return new DirectExchange(properties.exchange());
    }

    // =========================================================
    // ===== SEND NOTIFICATION TO USER =====
    @Bean
    Queue sendUserQueue() {
        return QueueBuilder.durable(properties.queue().sendUser())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange()) //send mail fail, push to this exchage a gain with the routing below
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().retryUser())
                .build();
    }

    @Bean
    Queue retryUserQueue(){
        return QueueBuilder.durable(properties.queue().retryUser())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().sendUser())
                .build();
    }

    @Bean
    Queue dlqUserQueue(){
        return QueueBuilder.durable(properties.queue().dlqUser()).build();
    }


    @Bean
    Binding sendUserBinding() {
        return BindingBuilder
                .bind(sendUserQueue())
                .to(notificationExchange())
                .with(properties.routing().sendUser());
    }

    @Bean
    Binding retryUserBinding() {
        return BindingBuilder
                .bind(retryUserQueue())
                .to(notificationExchange())
                .with(properties.routing().retryUser());
    }

    @Bean
    Binding dlqUserBinding() {
        return BindingBuilder
                .bind(dlqUserQueue())
                .to(notificationExchange())
                .with(properties.routing().dlqUser());
    }

    // =========================================================
    // ===== SEND NOTIFICATION TO TOPIC =====
    @Bean
    Queue sendTopicQueue() {
        return QueueBuilder.durable(properties.queue().sendTopic())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange()) //send mail fail, push to this exchage a gain with the routing below
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().retryTopic())
                .build();
    }

    @Bean
    Queue retryTopicQueue(){
        return QueueBuilder.durable(properties.queue().retryTopic())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().sendTopic())
                .build();
    }

    @Bean
    Queue dlqTopicQueue(){
        return QueueBuilder.durable(properties.queue().dlqTopic()).build();
    }

    @Bean
    Binding sendTopicBinding() {
        return BindingBuilder
                .bind(sendTopicQueue())
                .to(notificationExchange())
                .with(properties.routing().sendTopic());
    }

    @Bean
    Binding retryTopicBinding() {
        return BindingBuilder
                .bind(retryTopicQueue())
                .to(notificationExchange())
                .with(properties.routing().retryTopic());
    }

    @Bean
    Binding dlqTopicBinding() {
        return BindingBuilder
                .bind(dlqTopicQueue())
                .to(notificationExchange())
                .with(properties.routing().dlqTopic());
    }

    // =========================================================
    // ===== SUBSCRIBE SINGLE TOKEN TO TOPICS =====
    @Bean
    Queue subscribeSingleTokenToTopicsQueue(){
        return QueueBuilder.durable(properties.queue().subscribeTokenTopics())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().subscribeTokenTopicsRetry())
                .build();
    }

    @Bean
    Queue subscribeSingleTokenToTopicsRetryQueue(){
        return QueueBuilder.durable(properties.queue().subscribeTokenTopicsRetry())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().subscribeTokenTopics())
                .build();
    }

    @Bean
    Queue subscribeSingleTokenToTopicsDlqQueue(){
        return QueueBuilder.durable(properties.queue().subscribeTokenTopicsDlq())
                .build();
    }

    @Bean
    Binding subscribeSingleTokenToTopicsBinding() {
        return BindingBuilder
                .bind(subscribeSingleTokenToTopicsQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeTokenTopics());
    }

    @Bean
    Binding subscribeSingleTokenToTopicsRetryBinding() {
        return BindingBuilder
                .bind(subscribeSingleTokenToTopicsRetryQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeTokenTopicsRetry());
    }

    @Bean
    Binding subscribeSingleTokenToTopicsDlqBinding() {
        return BindingBuilder
                .bind(subscribeSingleTokenToTopicsDlqQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeTokenTopicsDlq());
    }

    // =========================================================
    // ===== UNSUBSCRIBE SINGLE TOKEN FROM TOPICS =====
    @Bean
    Queue unsubscribeSingleTokenFromTopicsQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeTokenTopics())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().unsubscribeTokenTopicsRetry())
                .build();
    }

    @Bean
    Queue unsubscribeSingleTokenFromTopicsRetryQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeTokenTopicsRetry())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().unsubscribeTokenTopics())
                .build();
    }

    @Bean
    Queue unsubscribeSingleTokenFromTopicsDlqQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeTokenTopicsDlq())
                .build();
    }

    @Bean
    Binding unsubscribeSingleTokenFromTopicsBinding() {
        return BindingBuilder
                .bind(unsubscribeSingleTokenFromTopicsQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeTokenTopics());
    }

    @Bean
    Binding unsubscribeSingleTokenFromTopicsRetryBinding() {
        return BindingBuilder
                .bind(unsubscribeSingleTokenFromTopicsRetryQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeTokenTopicsRetry());
    }

    @Bean
    Binding unsubscribeSingleTokenFromTopicsDlqBinding() {
        return BindingBuilder
                .bind(unsubscribeSingleTokenFromTopicsDlqQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeTokenTopicsDlq());
    }

    // =========================================================
    // ===== SUBSCRIBE USER TO SINGLE TOPIC =====
    @Bean
    Queue subscribeUserToTopicQueue(){
        return QueueBuilder.durable(properties.queue().subscribeUserTopic())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().subscribeUserTopicRetry())
                .build();
    }

    @Bean
    Queue subscribeUserToTopicRetryQueue(){
        return QueueBuilder.durable(properties.queue().subscribeUserTopicRetry())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().subscribeUserTopic())
                .build();
    }

    @Bean
    Queue subscribeUserToTopicDlqQueue(){
        return QueueBuilder.durable(properties.queue().subscribeUserTopicDlq())
                .build();
    }

    @Bean
    Binding subscribeUserToTopicBinding() {
        return BindingBuilder
                .bind(subscribeUserToTopicQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeUserTopic());
    }

    @Bean
    Binding subscribeUserToTopicRetryBinding() {
        return BindingBuilder
                .bind(subscribeUserToTopicRetryQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeUserTopicRetry());
    }

    @Bean
    Binding subscribeUserToTopicDlqBinding() {
        return BindingBuilder
                .bind(subscribeUserToTopicDlqQueue())
                .to(notificationExchange())
                .with(properties.routing().subscribeUserTopicDlq());
    }

    // =========================================================
    // ===== UNSUBSCRIBE USER FROM SINGLE TOPIC =====
    @Bean
    Queue unsubscribeUserToTopicQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeUserTopic())
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().unsubscribeUserTopicRetry())
                .build();
    }

    @Bean
    Queue unsubscribeUserToTopicRetryQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeUserTopicRetry())
                .withArgument(HEADER_MESSAGE_TTL, properties.retry().ttl()) //milisecond
                .withArgument(HEADER_DEAD_LETTER_EXCHANGE, properties.exchange())
                .withArgument(HEADER_DEAD_LETTER_ROUTING_KEY, properties.routing().unsubscribeUserTopic())
                .build();
    }

    @Bean
    Queue unsubscribeUserToTopicDlqQueue(){
        return QueueBuilder.durable(properties.queue().unsubscribeUserTopicDlq())
                .build();
    }

    @Bean
    Binding unsubscribeUserToTopicBinding() {
        return BindingBuilder
                .bind(unsubscribeUserToTopicQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeUserTopic());
    }

    @Bean
    Binding unsubscribeUserToTopicRetryBinding() {
        return BindingBuilder
                .bind(unsubscribeUserToTopicRetryQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeUserTopicRetry());
    }

    @Bean
    Binding unsubscribeUserToTopicDlqBinding() {
        return BindingBuilder
                .bind(unsubscribeUserToTopicDlqQueue())
                .to(notificationExchange())
                .with(properties.routing().unsubscribeUserTopicDlq());
    }
}
