package com.sep490.g28.hvh.be.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq.notification")
public record RabbitMqNotificationProperties(

        String exchange,
        Queue queue,
        Routing routing,
        Retry retry

) {

    public record Queue(
            String sendUser,
            String sendTopic,
            String retryUser,
            String retryTopic,
            String dlqUser,
            String dlqTopic,

            String subscribeTokenTopics,
            String subscribeTokenTopicsRetry,
            String subscribeTokenTopicsDlq,

            String unsubscribeTokenTopics,
            String unsubscribeTokenTopicsRetry,
            String unsubscribeTokenTopicsDlq


            ) {}

    public record Routing(
            String sendUser,
            String sendTopic,
            String retryUser,
            String retryTopic,
            String dlqUser,
            String dlqTopic,

            String subscribeTokenTopics,
            String subscribeTokenTopicsRetry,
            String subscribeTokenTopicsDlq,

            String unsubscribeTokenTopics,
            String unsubscribeTokenTopicsRetry,
            String unsubscribeTokenTopicsDlq
    ) {}

    public record Retry(
            int ttl,
            int maxAttempts
    ) {}
}
