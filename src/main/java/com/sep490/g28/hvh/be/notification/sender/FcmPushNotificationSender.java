package com.sep490.g28.hvh.be.notification.sender;

import com.google.firebase.messaging.*;
import com.sep490.g28.hvh.be.notification.constant.EFcmFailureType;
import com.sep490.g28.hvh.be.notification.dto.SendNotificationMessage;
import com.sep490.g28.hvh.be.notification.exception.NonRetryableFcmException;
import com.sep490.g28.hvh.be.notification.repository.NotificationTokenRepository;
import com.sep490.g28.hvh.be.notification.service.NotificationTokenTxService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) implementation of {@link PushNotificationSender}.
 *
 * <p>Responsible for delivering push notifications and managing topic
 * subscriptions using the Firebase Admin SDK.</p>
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class FcmPushNotificationSender implements PushNotificationSender {

    NotificationTokenTxService notificationTokenTxService;
    /** FCM maximum number of tokens per multicast request. */
    private static final int BATCH_SIZE = 500;
    private final NotificationTokenRepository notificationTokenRepository;

    /**
     * Classify {@link FirebaseMessagingException} into retryable or non-retryable.
     *
     * <p>Also removes permanently invalid tokens (UNREGISTERED,
     * SENDER_ID_MISMATCH, INVALID_ARGUMENT).</p>
     *
     * @param e     Firebase exception
     * @param token related token (nullable)
     * @return failure classification
     */
    public EFcmFailureType classifyFcmFailureType(FirebaseMessagingException e, String token) {

        MessagingErrorCode code = e.getMessagingErrorCode();

        return switch (code) {
            //token dead, invalid token/payload
            case UNREGISTERED, SENDER_ID_MISMATCH, INVALID_ARGUMENT -> {
                if (token != null && !token.isBlank()) {
                    notificationTokenTxService.deleteToken(token);
                }
                yield EFcmFailureType.NON_RETRYABLE;
            }
            // temporal error, rate/quota
            case UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED -> EFcmFailureType.RETRYABLE;
            // invalid auth/config
            case THIRD_PARTY_AUTH_ERROR -> {
                log.error("FCM auth/config error");
                yield EFcmFailureType.NON_RETRYABLE;
            }
        };
    }

    /**
     * Send notification to all tokens belonging to a specific user.
     *
     * <p>Tokens are fetched from database and split into batches of
     * {@code BATCH_SIZE} (max 500 tokens per FCM request).</p>
     *
     * <p>Invalid tokens detected in batch response are removed.</p>
     *
     * @param notification notification message payload
     */
    @Override
    public void sendMulticast(SendNotificationMessage notification) {
        List<String> tokens = notificationTokenRepository.findTokensByUserId(notification.getUserId());
        //check tokens list
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        //split into batches
        for (int i = 0; i < tokens.size(); i += BATCH_SIZE) {
            List<String> batch =
                    tokens.subList(i, Math.min(i + BATCH_SIZE, tokens.size()));
            //create batch message
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(
                            Notification.builder()
                                    .setTitle(notification.getTitle())
                                    .setBody(notification.getBody())
                                    .build()
                    )
                    .putAllData(
                            notification.getData() == null ? Map.of() : notification.getData()
                    )
                    .build();

            try {
                //send batch message
                BatchResponse response =
                        FirebaseMessaging.getInstance().sendEachForMulticast(message);

                log.info(
                        "Sent multicast: notificationId={}, success={}, failure={}",
                        notification.getNotificationId(),
                        response.getSuccessCount(),
                        response.getFailureCount()
                );

                //has some messages sent fail, delete respective tokens
                if (response.getFailureCount() > 0) {
                    List<String> invalidTokens = getInvalidTokens(tokens, response);
                    notificationTokenTxService.deleteInvalidTokens(invalidTokens);
                    log.warn("Delete invalid tokens: {}", invalidTokens);
                }

            } catch (FirebaseMessagingException e) {
                log.error("FCM send multicast failed: notificationId={}", notification.getNotificationId(), e);
                EFcmFailureType type = classifyFcmFailureType(e, null);

                if (type == EFcmFailureType.RETRYABLE) {
                    throw new RuntimeException("FCM_RETRYABLE");
                } else {
                    throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
                }
            }
        }
    }

    /**
     * Extract permanently invalid tokens from FCM batch response.
     *
     * <p>Only non-retryable error codes are considered invalid.</p>
     *
     * @param tokens   original token list (same order as request)
     * @param response FCM batch response
     * @return list of invalid tokens
     */
    private static List<String> getInvalidTokens(List<String> tokens, BatchResponse response) {
        List<String> invalidTokens = new ArrayList<>();

        for (int i = 0; i < response.getResponses().size(); i++) {
            SendResponse r = response.getResponses().get(i);
            if (!r.isSuccessful()) {
                String errorCode = r.getException().getMessagingErrorCode().name();
                if (errorCode.equals("UNREGISTERED")
                        || errorCode.equals("INVALID_ARGUMENT")
                        || errorCode.equals("SENDER_ID_MISMATCH")) {
                    invalidTokens.add(tokens.get(i));
                }
            }
        }
        return invalidTokens;
    }

    /**
     * Send notification to a specific FCM topic.
     *
     * @param notification notification message payload
     */
    @Override
    public void sendToTopic(SendNotificationMessage notification) {
        Message msg = Message.builder()
                .setTopic(notification.getTopic())
                .setNotification(
                        Notification.builder()
                                .setTitle(notification.getTitle())
                                .setBody(notification.getBody())
                                .build()
                )
                .putAllData(
                        notification.getData() == null ? Map.of() : notification.getData()
                )
                .build();
        try {
            String msgId = FirebaseMessaging.getInstance().send(msg);
            log.info("Send message to topic={}, notificationId={}, msgId={}",
                    notification.getTopic(),
                    notification.getNotificationId(),
                    msgId
            );
        } catch (FirebaseMessagingException e) {
            log.error("FCM send to topic failed: topic={}, notificationId={}",
                    notification.getNotificationId(),
                    notification.getTopic(),
                    e
            );
            EFcmFailureType type = classifyFcmFailureType(e, null);

            if (type == EFcmFailureType.RETRYABLE) {
                throw new RuntimeException("FCM_RETRYABLE");
            } else {
                throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
            }
        }
    }

    /**
     * Subscribe a device token to multiple topics.
     *
     * @param token  device token
     * @param topics collection of topic names
     */
    @Override
    public void subscribeToTopics(String token, Collection<String> topics) {
        if (token == null || topics == null || topics.isEmpty()) return;

        try {
            for (String topic : topics) {
                FirebaseMessaging.getInstance()
                        .subscribeToTopic(List.of(token), topic);
            }
            log.info("Subscribed token={} to topics={}", token, topics);
        } catch (FirebaseMessagingException e) {
            log.error("FCM subscribe token to topics failed token={} topics={}", token, topics, e);
            EFcmFailureType type = classifyFcmFailureType(e, null);

            if (type == EFcmFailureType.RETRYABLE) {
                throw new RuntimeException("FCM_RETRYABLE");
            } else {
                throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
            }
        }
    }

    /**
     * Unsubscribe a device token from multiple topics.
     *
     * @param token  device token
     * @param topics collection of topic names
     */
    @Override
    public void unsubscribeFromTopics(String token, Collection<String> topics) {
        if (token == null || topics == null || topics.isEmpty()) return;

        try {
            for (String topic : topics) {
                FirebaseMessaging.getInstance()
                        .unsubscribeFromTopic(List.of(token), topic);
            }
            log.info("Unsubscribed token={} from topics={}", token, topics);
        } catch (FirebaseMessagingException e) {
            log.error("FCM unsubscribe token from topics failed token={} topics={}", token, topics, e);
            EFcmFailureType type = classifyFcmFailureType(e, null);

            if (type == EFcmFailureType.RETRYABLE) {
                throw new RuntimeException("FCM_RETRYABLE");
            } else {
                throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
            }
        }
    }



    public void subscribeTokensToTopic(Collection<String> tokens, String topic) {
        if (tokens == null || tokens.isEmpty() || topic == null) return;

        List<List<String>> batches = partition(tokens, BATCH_SIZE);

        for (List<String> batch : batches) {
            try {
                FirebaseMessaging.getInstance()
                        .subscribeToTopic(batch, topic);

                log.info("Subscribed {} tokens to topic={}", batch.size(), topic);

            } catch (FirebaseMessagingException e) {
                log.error("FCM subscribeTokenTopics tokens from topic failed topic={}", topic, e);

                EFcmFailureType type = classifyFcmFailureType(e, null);

                if (type == EFcmFailureType.RETRYABLE) {
                    throw new RuntimeException("FCM_RETRYABLE");
                } else {
                    throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
                }
            }
        }
    }

    public void unsubscribeTokensFromTopic(Collection<String> tokens, String topic) {
        if (tokens == null || tokens.isEmpty() || topic == null) return;

        List<List<String>> batches = partition(tokens, BATCH_SIZE);

        for (List<String> batch : batches) {
            try {
                FirebaseMessaging.getInstance()
                        .unsubscribeFromTopic(batch, topic);

                log.info("Unsubscribed {} tokens from topic={}", batch.size(), topic);

            } catch (FirebaseMessagingException e) {
                log.error("FCM unsubscribeTokenTopics tokens from topic failed topic={}", topic, e);

                EFcmFailureType type = classifyFcmFailureType(e, null);

            if (type == EFcmFailureType.RETRYABLE) {
                throw new RuntimeException("FCM_RETRYABLE");
            } else {
                throw new NonRetryableFcmException("FCM_NON_RETRYABLE");
            }
        }
    }
}
