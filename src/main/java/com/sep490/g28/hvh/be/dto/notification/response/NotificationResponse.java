package com.sep490.g28.hvh.be.dto.notification.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class NotificationResponse {
    UUID notificationId;
    String title;
    String body;
    Map<String, String> data;
//    boolean isRead;
//    OffsetDateTime readAt;
    OffsetDateTime createdAt;

    public NotificationResponse(
            UUID notificationId,
            String title,
            String body,
            Map<String, String> data,
            OffsetDateTime createdAt
    ) {
        this.notificationId = notificationId;
        this.title = title;
        this.body = body;
        this.data = data;
        this.createdAt = createdAt;
    }
}
