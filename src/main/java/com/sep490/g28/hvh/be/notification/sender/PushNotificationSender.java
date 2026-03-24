package com.sep490.g28.hvh.be.notification.sender;

import com.sep490.g28.hvh.be.notification.dto.SendNotificationMessage;

import java.util.Collection;
import java.util.UUID;

/**
 * Client abstraction for sending push notifications.
 *
 * <p>Implementations are responsible for handling provider-specific
 * details (e.g. Firebase Cloud Messaging).</p>
 */
public interface PushNotificationSender {

    /**
     * Send a notification to multiple device tokens.
     *
     * <p>Implementations must handle provider limits
     * (e.g. FCM max 500 tokens per request).</p>
     *
     * @param notification the notification that will be sent
     */
    void sendMulticast(SendNotificationMessage notification);

    /**
     * Send a notification to all devices subscribed to a topic.
     *
     * @param notification the notification that will be sent
     */
    void sendToTopic(SendNotificationMessage notification);

    /**
     * Subscribe a device token to multiple topics.
     *
     * @param token device token
     * @param topics topics' name
     */
    void subscribeSingleTokenToTopics(String token, Collection<String> topics);

    /**
     * Unsubscribe a device token to multiple topics.
     *
     * @param token device token
     * @param topics topics' name
     */
    void unsubscribeSingleTokenFromTopics(String token, Collection<String> topics);

    void subscribeUserToTopic(UUID userId, String topicName);

    void unsubscribeUserFromTopic(UUID userId, String topicName);
}
