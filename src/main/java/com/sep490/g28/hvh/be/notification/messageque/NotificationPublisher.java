package com.sep490.g28.hvh.be.notification.messageque;

import com.sep490.g28.hvh.be.notification.config.RabbitMqNotificationProperties;
import com.sep490.g28.hvh.be.notification.dto.SendNotificationMessage;
import com.sep490.g28.hvh.be.notification.dto.TopicSubscriptionMessage;
import com.sep490.g28.hvh.be.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

/**
 * Publisher responsible for enqueueing notification-related messages
 * to RabbitMQ for asynchronous processing.
 *
 * <p>This service does not send push notifications directly. Instead,
 * it publishes messages to specific routing keys so that downstream
 * consumers can handle delivery (e.g. FCM send, topic subscription).</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Publish notification send requests (user or topic based)</li>
 *   <li>Publish topic subscription requests</li>
 *   <li>Publish topic unsubscription requests</li>
 * </ul>
 *
 */
@Service
@RequiredArgsConstructor
public class NotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqNotificationProperties properties;

    /**
     * Enqueue a notification send request.
     *
     * <p>Builds a {@code SendNotificationMessage} and publishes it to
     * the appropriate routing key depending on whether the notification
     * targets a topic or a specific user.</p>
     *
     * <p>Routing behavior:</p>
     * <ul>
     *   <li>If {@code notification.topic} is present → route to topic-send queue</li>
     *   <li>If {@code targetUserId} is present → route to user-send queue</li>
     *   <li>If both are missing → throw {@link IllegalArgumentException}</li>
     * </ul>
     *
     * @param notification  notification domain object
     * @param targetUserId  target user ID (nullable if sending by topic)
     * @throws IllegalArgumentException if both topic and targetUserId are missing
     */
    public void enqueueNotification(Notification notification, UUID targetUserId) {
        SendNotificationMessage payload = SendNotificationMessage.builder()
                .notificationId(notification.getId())
                .userId(targetUserId)
                .topic(notification.getTopic())
                .title(notification.getTitle())
                .body(notification.getBody())
                .data(notification.getData())
                .build();

        if (notification.getTopic() != null && !notification.getTopic().isBlank()) {
            //put message to queue send user
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    properties.routing().sendTopic(),
                    payload
            );
        } else if (targetUserId != null) {
            //put message to queue send topic
            rabbitTemplate.convertAndSend(
                    properties.exchange(),
                    properties.routing().sendUser(),
                    payload
            );
        } else throw new IllegalArgumentException("Missing target in notification, check your code");
    }

    /**
     * Enqueue a request to subscribeTokenTopics a device token to multiple topics.
     *
     * @param token  device token
     * @param topics collection of topic names
     */
    public void enqueueSubscribeToTopics(String token, Collection<String> topics) {

        TopicSubscriptionMessage payload =
                new TopicSubscriptionMessage(token, topics);

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routing().subscribeTokenTopics(),
                payload
        );
    }

    /**
     * Enqueue a request to unsubscribeTokenTopics a device token from multiple topics.
     *
     * @param token  device token
     * @param topics collection of topic names
     */
    public void enqueueUnsubscribeFromTopics(String token, Collection<String> topics) {

        TopicSubscriptionMessage payload =
                new TopicSubscriptionMessage(token, topics);

        rabbitTemplate.convertAndSend(
                properties.exchange(),
                properties.routing().unsubscribeTokenTopics(),
                payload
        );
    }

}
