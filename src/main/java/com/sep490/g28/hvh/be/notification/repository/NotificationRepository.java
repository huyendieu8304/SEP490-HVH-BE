package com.sep490.g28.hvh.be.notification.repository;

import com.sep490.g28.hvh.be.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
    SELECT n
    FROM Notification n
    JOIN NotificationTopicSubscription s
        ON s.topic = n.topic
    WHERE s.user.id = :userId
    ORDER BY n.createdAt DESC
""")
    Page<Notification> findNotificationsByUserTopics(UUID userId, Pageable pageable);
}
