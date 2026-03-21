package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.notification.dto.RegisterNotificationTokenRequest;

import java.util.UUID;

public interface NotificationService {

    void registerNotificationToken(RegisterNotificationTokenRequest request);

    void unregisterNotificationToken(String token);

    void subscribeUserToTopicOfEvent(UUID userId, UUID eventId);

    void unsubscribeUserFromTopicOfEvent(UUID userId, UUID eventId);

//todo
//    List<UserNotification> getLatestNotification(OffsetDateTime cursor);

    void sendEventCreatedNotification(Event event, Host host);
    void sendEventApprovedByOrgManagerNotification(Event event);
    void sendEventRejectedByOrgManagerNotification(Event event, String reason);

    void sendEventApprovedByAdminNotification(Event event);
    void sendEventRejectedByAdminNotification(Event event, String reason);

    void sendEventApplicationApproved(UUID volunteerId, Event event, EventApplication application);
    void sendEventApplicationRejected(UUID volunteerId, Event event, EventApplication application, String rejectionReason);
    void sendEventApplicationCancelledSuccessfully(UUID volunteerId, Event event, EventApplication application, boolean isMinusScore);
}
