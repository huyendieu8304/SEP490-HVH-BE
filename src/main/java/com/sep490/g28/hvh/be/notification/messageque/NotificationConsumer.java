package com.sep490.g28.hvh.be.notification.messageque;

import com.sep490.g28.hvh.be.notification.config.RabbitMqNotificationProperties;
import com.sep490.g28.hvh.be.notification.dto.SendNotificationMessage;
import com.sep490.g28.hvh.be.notification.dto.TokenToTopicsSubscriptionMessage;
import com.sep490.g28.hvh.be.notification.dto.UserToTopicSubscriptionMessage;
import com.sep490.g28.hvh.be.notification.exception.NonRetryableFcmException;
import com.sep490.g28.hvh.be.notification.sender.PushNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RabbitMQ consumer responsible for processing notification-related messages.
 *
 * <p>This component listens to multiple queues for:
 * <ul>
 *   <li>Sending notifications to users</li>
 *   <li>Sending notifications to topics</li>
 *   <li>Subscribing tokens to topics</li>
 *   <li>Unsubscribing tokens from topics</li>
 * </ul>
 *
 * <p>It implements a retry mechanism using RabbitMQ dead-letter exchanges (DLX)
 * and the {@code x-death} header to track retry attempts.</p>
 *
 * <p>Behavior rules:</p>
 * <ul>
 *   <li>Retry until {@code maxAttempts} is reached</li>
 *   <li>Immediately route to DLQ on {@link NonRetryableFcmException}</li>
 *   <li>Log permanently failed messages in DLQ consumers</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final PushNotificationSender pushNotificationSender;
    private final RabbitMqNotificationProperties properties;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Extract total retry attempts for a specific queue using the
     * {@code x-death} header provided by RabbitMQ.
     *
     * @param message   raw AMQP message
     * @param queueName queue to inspect
     * @return total retry count for that queue
     */
    private int getRetryCountForQueue(Message message, String queueName) {

        List<Map<String, Object>> deaths =
                (List<Map<String, Object>>) message
                        .getMessageProperties()
                        .getHeaders()
                        .get("x-death");

        if (deaths == null) return 0;

        log.info(deaths.toString());

        return deaths.stream()
                .filter(d -> queueName.equals(d.get("queue")))
                .mapToInt(d -> ((Long) d.get("count")).intValue())
                .sum();
    }

    // =========================================================
    // ===== SEND NOTIFICATION TO USER =====
    /**
     * Consume user-targeted notification messages.
     *
     * <p>Retries on generic exceptions. Moves message to DLQ if:
     * <ul>
     *   <li>Max retry attempts exceeded</li>
     *   <li>{@link NonRetryableFcmException} is thrown</li>
     * </ul>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.send-user}")
    public void consumeSendUser(
            Message message,
            SendNotificationMessage notification) {

        int retryCount = getRetryCountForQueue(message, properties.queue().sendUser());
        if (retryCount >= properties.retry().maxAttempts() - 1) {
            // exceed max attempts -> send to dlq
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().dlqUser(),
                    message
            );
            return;
        }

        try {
            //retry send notification
            pushNotificationSender.sendMulticast(notification);
        } catch (NonRetryableFcmException e) {

            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().dlqUser(),
                    message
            );

        } catch (Exception e) {
            throw new AmqpRejectAndDontRequeueException("RETRY");
        }
    }

    /**
     * Consume dead-lettered user notification messages.
     *
     * <p>Logs final failure after all retry attempts.</p>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.dlq-user}")
    public void consumeDlqUser(Message message, SendNotificationMessage notification) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().dlqUser());

        log.error(
                "NOTIFICATION SENT FAILED after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: notification={}", notification);
    }

    // =========================================================
    // ===== SEND NOTIFICATION TO TOPIC =====
    /**
     * Consume topic-targeted notification messages.
     *
     * <p>Retries on generic exceptions. Moves message to DLQ if:
     * <ul>
     *   <li>Max retry attempts exceeded</li>
     *   <li>{@link NonRetryableFcmException} is thrown</li>
     * </ul>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.send-topic}")
    public void consumeSendTopic(
            Message message,
            SendNotificationMessage notification) {

        int retryCount = getRetryCountForQueue(message, properties.queue().sendTopic());

        try {
            //retry send notification
            pushNotificationSender.sendToTopic(notification);
        } catch (NonRetryableFcmException e) {
            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().dlqTopic(),
                    message
            );

        } catch (Exception e) {
            if (retryCount >= properties.retry().maxAttempts() - 1) {
                // exceed max attempts -> send to dlq
                rabbitTemplate.send(
                        properties.exchange(),
                        properties.routing().dlqTopic(),
                        message
                );
            }else {
                throw new AmqpRejectAndDontRequeueException("RETRY");
            }
        }
    }

    /**
     * Consume dead-lettered topic notification messages.
     *
     * <p>Logs permanent failure information.</p>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.dlq-topic}")
    public void consumeDlqTopic(Message message, SendNotificationMessage notification) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().dlqUser());

        log.error(
                "NOTIFICATION SENT TO TOPIC FAILED after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: notification={}", notification);
    }


    // =========================================================
    // ===== SUBSCRIBE SINGLE TOKEN TO TOPICS =====
    /**
     * Consume topics subscription requests.
     *
     * <p>Retries on transient errors and routes to DLQ on:
     * <ul>
     *   <li>Non-retryable FCM errors</li>
     *   <li>Exceeded retry attempts</li>
     * </ul>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-token-topics}")
    public void consumeSubscribeSingleTokenToTopics(
            Message message,
            TokenToTopicsSubscriptionMessage msg) {

        int retryCount =
                getRetryCountForQueue(message, properties.queue().subscribeTokenTopics());

        try {
            pushNotificationSender.subscribeSingleTokenToTopics(msg.getToken(), msg.getTopics());
        } catch (NonRetryableFcmException e) {
            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().subscribeTokenTopicsDlq(),
                    message
            );

        } catch (Exception e) {
            if (retryCount >= properties.retry().maxAttempts() - 1) {
                // exceed max attempts -> send to dlq
                rabbitTemplate.send(
                        properties.exchange(),
                        properties.routing().subscribeTokenTopicsDlq(),
                        message
                );
            } else {
                throw new AmqpRejectAndDontRequeueException("RETRY");
            }
        }
    }

    /**
     * Consume dead-lettered subscription messages.
     *
     * <p>Logs final failure after retries exhausted.</p>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-token-topics-dlq}")
    public void consumeSubscribeSingleTokenToTopicsDlq(Message message, TokenToTopicsSubscriptionMessage msg) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().subscribeTokenTopicsDlq());

        log.error(
                "SUBSCRIBE SINGLE TOKEN TO TOPICS FAIL after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: {}", msg);
    }


    // =========================================================
    // ===== UNSUBSCRIBE SINGLE TOKEN FROM TOPICS =====
    /**
     * Consume topics unsubscription requests.
     *
     * <p>Retries on transient errors and routes to DLQ on:
     * <ul>
     *   <li>Non-retryable FCM errors</li>
     *   <li>Exceeded retry attempts</li>
     * </ul>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.unsubscribe-token-topics}")
    public void consumeUnsubscribeSingleTokenToTopics(
            Message message,
            TokenToTopicsSubscriptionMessage msg) {

        int retryCount =
                getRetryCountForQueue(message, properties.queue().unsubscribeTokenTopics());

        try {
            pushNotificationSender.unsubscribeSingleTokenFromTopics(msg.getToken(), msg.getTopics());
        } catch (NonRetryableFcmException e) {
            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().unsubscribeTokenTopicsDlq(),
                    message
            );

        } catch (Exception e) {
            if (retryCount >= properties.retry().maxAttempts() - 1) {
                // exceed max attempts -> send to dlq
                rabbitTemplate.send(
                        properties.exchange(),
                        properties.routing().unsubscribeTokenTopicsDlq(),
                        message
                );
            } else {
                throw new AmqpRejectAndDontRequeueException("RETRY");
            }
        }
    }

    /**
     * Consume dead-lettered unsubscription messages.
     *
     * <p>Logs permanent failure information.</p>
     */
    @RabbitListener(queues = "${rabbitmq.notification.queue.unsubscribe-token-topics-dlq}")
    public void consumeUnsubscribeSingleTokenToTopicsDlq(Message message, TokenToTopicsSubscriptionMessage msg) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().unsubscribeTokenTopicsDlq());

        log.error(
                "UNSUBSCRIBE SINGLE TOKEN TO TOPICS FAIL after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: {}", msg);
    }

    // =========================================================
    // ===== SUBSCRIBE USER TO SINGLE TOPIC =====
    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-user-topic}")
    public void consumeSubscribeUserToTopics(Message message, UserToTopicSubscriptionMessage msg) {
        int retryCount = getRetryCountForQueue(message, properties.queue().subscribeUserTopic());

        try {
            pushNotificationSender.subscribeUserToTopic(msg.getUserId(), msg.getTopic());
        } catch (NonRetryableFcmException e) {
            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().subscribeUserTopicDlq(),
                    message
            );

        } catch (Exception e) {
            if (retryCount >= properties.retry().maxAttempts() - 1) {
                // exceed max attempts -> send to dlq
                rabbitTemplate.send(
                        properties.exchange(),
                        properties.routing().subscribeUserTopicDlq(),
                        message
                );
            } else {
                throw new AmqpRejectAndDontRequeueException("RETRY");
            }
        }
    }

    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-user-topic-dlq}")
    public void consumeSubscribeUserToTopicsDlq(Message message, UserToTopicSubscriptionMessage msg) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().unsubscribeUserTopicDlq());

        log.error(
                "SUBSCRIBE USER TO TOPIC FAIL after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: {}", msg);
    }

    // =========================================================
    // ===== UNSUBSCRIBE USER FROM SINGLE TOPIC =====

    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-user-topic}")
    public void consumeUnsubscribeUserToTopics(Message message, UserToTopicSubscriptionMessage msg) {
        int retryCount = getRetryCountForQueue(message, properties.queue().unsubscribeUserTopic());

        try {
            pushNotificationSender.unsubscribeUserFromTopic(msg.getUserId(), msg.getTopic());
        } catch (NonRetryableFcmException e) {
            // send to dlq immediately
            rabbitTemplate.send(
                    properties.exchange(),
                    properties.routing().unsubscribeUserTopicDlq(),
                    message
            );

        } catch (Exception e) {
            if (retryCount >= properties.retry().maxAttempts() - 1) {
                // exceed max attempts -> send to dlq
                rabbitTemplate.send(
                        properties.exchange(),
                        properties.routing().unsubscribeUserTopicDlq(),
                        message
                );
            } else {
                throw new AmqpRejectAndDontRequeueException("RETRY");
            }
        }
    }

    @RabbitListener(queues = "${rabbitmq.notification.queue.subscribe-user-topic-dlq}")
    public void consumeUnsubscribeUserToTopicsDlq(Message message, UserToTopicSubscriptionMessage msg) {
        MessageProperties props = message.getMessageProperties();

        int retryCount = getRetryCountForQueue(message, properties.queue().unsubscribeUserTopicDlq());

        log.error(
                "UNSUBSCRIBE USER FROM TOPIC FAIL after {} retries, reason={}",
                retryCount,
                props.getHeaders().get("x-first-death-reason")
        );
        log.error("DLQ MESSAGE: {}", msg);
    }

}