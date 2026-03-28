package com.sep490.g28.hvh.be.notification.entity;

import com.sep490.g28.hvh.be.constant.ENotificationType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * The notification sent
 */
@Entity
@Table(name = "notifications",
        indexes = {
                @Index(name = "idx_notification_created", columnList = "created_at DESC")
        })
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue
    UUID id;

    String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ENotificationType type;

    @Column(nullable = false)
    String title;

    @Column(nullable = false, columnDefinition = "text")
    String body;

    @Type(JsonType.class)   // Hibernate 6
    @Column(columnDefinition = "jsonb")
    Map<String, String> data;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
