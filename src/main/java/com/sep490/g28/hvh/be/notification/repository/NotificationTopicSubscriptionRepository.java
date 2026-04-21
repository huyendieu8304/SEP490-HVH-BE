package com.sep490.g28.hvh.be.notification.repository;

import com.sep490.g28.hvh.be.notification.entity.NotificationTopicSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface NotificationTopicSubscriptionRepository extends JpaRepository<NotificationTopicSubscription, UUID> {
    @Query(
            value = """
                    select topic
                    from notification_topic_subscriptions
                    where user_id = :userId
                    """,
            nativeQuery = true
    )
    List<String> findTopicsByUserId(UUID userId);

    void deleteByUser_IdAndTopic(UUID userId, String topicName);
}
