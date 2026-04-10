package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.dto.notification.request.RegisterNotificationTokenRequest;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void registerNotificationToken(RegisterNotificationTokenRequest request);

    void unregisterNotificationToken(String token);

    void subscribeUserToTopicOfEvent(UUID userId, UUID eventId);

    void unsubscribeUserFromTopicOfEvent(UUID userId, UUID eventId);

//todo
//    List<UserNotification> getLatestNotification(OffsetDateTime cursor);

    void sendEventCreatedNotification(Event event, Host host);
    void sendEventCreateApprovedByOrgManagerNotification(Event event);
    void sendEventCreateRejectedByOrgManagerNotification(Event event, String reason);

    void sendEventCreateApprovedByAdminNotification(Event event);
    void sendEventCreateRejectedByAdminNotification(Event event, String reason);

    void sendEventApplicationApprovedNotification(UUID volunteerId, Event event, EventApplication application);
    void sendEventApplicationRejectedNotification(UUID volunteerId, Event event, EventApplication application, String rejectionReason);
    void sendEventApplicationCancelledSuccessfullyNotification(UUID volunteerId, Event event, EventApplication application, boolean isMinusScore);

    void sendNotificationToVolunteersOfEvent(UUID eventId, AnnounceVolunteerRequest request);

    void sentEventCancelledByHostNotification(List<EventApplication> eventApplications, String eventName, String cancelReason);
    void sentEventCancelledByAdminNotification(List<EventApplication> eventApplications, String eventName, String cancelReason);

    void sentEventUpdatedByHostNotification(UUID orgManagerId, UUID eventId, String eventName);

    void sendEventUpdateNonCriticalApprovedByOrgManagerNotification(UUID hostId, Event event);
    void sendEventUpdateNonCriticalApprovedByOrgManagerNotification(List<EventApplication> eventApplications, String eventName);

    void sendEventUpdateCriticalApprovedByOrgManagerNotification(UUID hostId, Event event);

    void sendEventUpdateRejectedByOrgManagerNotification(UUID hostId, Event event);

    void sendEventUpdateCriticalApprovedByAdminNotification(Event event);
    void sendEventUpdateCriticalApprovedByAdminNotification(List<EventApplication> eventApplications, String eventName);

    void sendEventUpdateCriticalRejectedByAdminNotification(Event event);

    void sendEventAssignedHostNotification(UUID oldHostId, UUID newHostId, Event event);

    void sendVolunteerReviewedByHostNotification(UUID volunteerId, Event event, EventApplication application, UUID reviewId);

    void sendVolunteerReceivedCertificateNotification(Volunteer volunteer, Event event);

    void sendVolunteersReceivedCertificatesNotifications(List<Volunteer> volunteers, Event event);

    void sendEventCompletedNotifications(Event event, UUID orgManagerId, UUID hostId);

    void sendCheckInCodeOfEventSessionNotifications(List<EventApplication> eventApplications, String eventName, String checkInCode);

    void sendEventSessionHostedTodayNotification(UUID hostId, UUID eventId, String eventName);
}
