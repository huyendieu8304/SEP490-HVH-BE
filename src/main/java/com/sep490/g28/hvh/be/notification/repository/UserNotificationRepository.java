package com.sep490.g28.hvh.be.notification.repository;


import com.sep490.g28.hvh.be.notification.entity.Notification;
import com.sep490.g28.hvh.be.notification.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    // first time load, no cursor
    @Query("""
        select un
        from UserNotification un
        join fetch un.notification n
        where un.user.id = :userId
        order by un.createdAt desc
    """)
    List<UserNotification> findFirstPage(
            @Param("userId") UUID userId,
            Pageable pageable
    );

    // continue to load with cursor
    @Query("""
        select un
        from UserNotification un
        join fetch un.notification n
        where un.user.id = :userId
          and un.createdAt < :cursor
        order by un.createdAt desc
    """)
    List<UserNotification> findNextPage(
            @Param("userId") UUID userId,
            @Param("cursor") OffsetDateTime cursor,
            Pageable pageable
    );

    @Query("""
                SELECT n
                FROM UserNotification un
                JOIN un.notification n
                WHERE un.user.id = :userId
                ORDER BY n.createdAt DESC
            """)
    Page<Notification> findUserNotificationByUserId(UUID userId, Pageable pageable);
}
