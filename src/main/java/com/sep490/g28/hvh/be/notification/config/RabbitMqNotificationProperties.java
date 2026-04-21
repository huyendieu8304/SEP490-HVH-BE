package com.sep490.g28.hvh.be.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Contain configuration properties relate to sending notification by rabbitMq
 * @param exchange exchanges for notifications
 * @param queue queue properties
 * @param routing routing properties
 * @param retry retry param properties
 */
@ConfigurationProperties(prefix = "rabbitmq.notification")
public record RabbitMqNotificationProperties(

        String exchange,
        Queue queue,
        Routing routing,
        Retry retry

) {

    public record Queue(
            String sendUser,
            String retryUser,
            String dlqUser,

            String sendTopic,
            String retryTopic,
            String dlqTopic,

            String subscribeTokenTopics,
            String subscribeTokenTopicsRetry,
            String subscribeTokenTopicsDlq,

            String unsubscribeTokenTopics,
            String unsubscribeTokenTopicsRetry,
            String unsubscribeTokenTopicsDlq,

            String subscribeUserTopic,
            String subscribeUserTopicRetry,
            String subscribeUserTopicDlq,

            String unsubscribeUserTopic,
            String unsubscribeUserTopicRetry,
            String unsubscribeUserTopicDlq


            ) {}

    public record Routing(
            String sendUser,
            String retryUser,
            String dlqUser,

            String sendTopic,
            String retryTopic,
            String dlqTopic,

            String subscribeTokenTopics,
            String subscribeTokenTopicsRetry,
            String subscribeTokenTopicsDlq,

            String unsubscribeTokenTopics,
            String unsubscribeTokenTopicsRetry,
            String unsubscribeTokenTopicsDlq,

            String subscribeUserTopic,
            String subscribeUserTopicRetry,
            String subscribeUserTopicDlq,

            String unsubscribeUserTopic,
            String unsubscribeUserTopicRetry,
            String unsubscribeUserTopicDlq
    ) {}

    public record Retry(
            int ttl,
            int maxAttempts
    ) {}
}
