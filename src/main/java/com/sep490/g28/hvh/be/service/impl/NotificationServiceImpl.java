package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.ENotificationDataAction;
import com.sep490.g28.hvh.be.constant.ENotificationType;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.notification.entity.Notification;
import com.sep490.g28.hvh.be.notification.entity.NotificationTopicSubscription;
import com.sep490.g28.hvh.be.notification.entity.UserNotification;
import com.sep490.g28.hvh.be.notification.entity.NotificationToken;
import com.sep490.g28.hvh.be.notification.messageque.NotificationPublisher;
import com.sep490.g28.hvh.be.notification.repository.UserNotificationRepository;
import com.sep490.g28.hvh.be.notification.repository.NotificationRepository;
import com.sep490.g28.hvh.be.notification.repository.NotificationTokenRepository;
import com.sep490.g28.hvh.be.dto.notification.request.RegisterNotificationTokenRequest;
import com.sep490.g28.hvh.be.notification.repository.NotificationTopicSubscriptionRepository;
import com.sep490.g28.hvh.be.notification.service.NotificationTokenTxService;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final UserRepository userRepository;
    private final NotificationTokenRepository notificationTokenRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTopicSubscriptionRepository notificationTopicSubscriptionRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final EventRepository eventRepository;

    private final CurrentUserProvider currentUserProvider;

    private final NotificationTokenTxService notificationTokenTxService;

    private final NotificationPublisher notificationPublisher;

    private static final String ORG_TOPIC_PRE = "org_"; //org_{orgId}
    private static final String EVENT_TOPIC_PRE = "event_"; //event_{eventId}
    private static final String ADMIN_TOPIC = "admin";

    private static final String DATA_REF_ID_KEY = "refId";
    private static final String DATA_ACTION = "action";
    private static final String DATA_NOTIFICATION_TYPE = "type";

    @Override
    public void registerNotificationToken(RegisterNotificationTokenRequest request) {
        UUID userId = currentUserProvider.getId();
        NotificationToken notificationToken = registerNotificationTokenInternal(request, userId);

        //subscribe token to topics
        subscribeTokenToTopicsAfterRegister(notificationToken.getToken(), userId);
    }

    public NotificationToken registerNotificationTokenInternal(RegisterNotificationTokenRequest request, UUID userId) {
        User user = userRepository.getReferenceById(userId);

        NotificationToken tokenEntity =
                notificationTokenRepository
                        .findByUserIdAndPlatformAndDeviceId(
                                user.getId(),
                                request.getPlatform().name(),
                                request.getDeviceId()
                        )
                        .orElseGet(NotificationToken::new);

        tokenEntity.setUser(user);
        tokenEntity.setPlatform(request.getPlatform());
        tokenEntity.setToken(request.getToken());
        tokenEntity.setDeviceId(request.getDeviceId());

        notificationTokenRepository.save(tokenEntity);
        log.info("Registered notification token: {}", tokenEntity);

        return tokenEntity;
    }

    private void subscribeTokenToTopicsAfterRegister(String token, UUID userId) {
        List<String> topics = notificationTopicSubscriptionRepository.findTopicsByUserId(userId);

        //todo
        //subscribe user to some special notification (in db) when created account
        //ADMIN: TO ADMIN_TOPIC
        //MANAGER:  to organization
        //HOST: to the organization

        //subscribe the to topic admin if the user is admin
        if (currentUserProvider.getRoleName().equals(ERole.SYS_ADMIN)) {
            topics.add(ADMIN_TOPIC);
        }
        notificationPublisher.enqueueSubscribeToTopics(token, topics);
    }

    @Override
    public void unregisterNotificationToken(String token) {
        UUID userId = currentUserProvider.getId();

        unregisterNotificationTokenInternal(token);

        unsubscribeTopicsAfterUnregister(token, userId);

    }

    public void unregisterNotificationTokenInternal(String token) {
        notificationTokenTxService.deleteToken(token);
        log.info("Unregistered notification token: {}", token);
    }

    private void unsubscribeTopicsAfterUnregister(String token, UUID userId) {
        List<String> topics = notificationTopicSubscriptionRepository.findTopicsByUserId(userId);

        //unsubscribe the to topic admin if the user is admin
        if (currentUserProvider.getRoleName().equals(ERole.SYS_ADMIN)) {
            topics.add(ADMIN_TOPIC);
        }
        notificationPublisher.enqueueUnsubscribeFromTopics(token, topics);
    }

    @Override
    public void subscribeUserToTopicOfEvent(UUID userId, UUID eventId) {
        String topicName = EVENT_TOPIC_PRE + eventId;

        NotificationTopicSubscription subscription = new NotificationTopicSubscription();
        subscription.setUser(userRepository.getReferenceById(userId));
        subscription.setTopic(topicName);
        notificationTopicSubscriptionRepository.save(subscription);

        //push request to message queue
        notificationPublisher.enqueueSubscribeUserToTopic(userId, topicName);
        log.info("Subscribed user to topic of event, userId={} evenId={} topic={}", userId, eventId, topicName);
    }

    @Transactional
    @Override
    public void unsubscribeUserFromTopicOfEvent(UUID userId, UUID eventId) {
        String topicName = EVENT_TOPIC_PRE + eventId;

        notificationTopicSubscriptionRepository.deleteByUser_IdAndTopic(userId, topicName);

        //push request to message queue
        notificationPublisher.enqueueUnsubscribeUserFromTopic(userId, topicName);
        log.info("Unsubscribed user from topic of event, userId={} evenId={} topic={}", userId, eventId, topicName);
    }

    //    @Override
//    public List<UserNotification> getLatestNotification(OffsetDateTime cursor) {
//        UUID currentUserId = currentUserProvider.getId();
//        Pageable pageable = PageRequest.of(0, 20);
//
//        if (cursor == null) {
//            return userNotificationRepository.findFirstPage(currentUserId, pageable);
//        }
//
//        return userNotificationRepository.findNextPage(currentUserId, cursor, pageable);
//    }

    //only used for send notification to user
    private Notification saveNotificationForUser(Notification notification, UUID userId) {
        //save notification
        notification = notificationRepository.save(notification);

        //link the notification to user in the db
        UserNotification userNotification = new UserNotification();
        userNotification.setNotification(notification);
        userNotification.setUser(userRepository.getReferenceById(userId));

        userNotificationRepository.save(userNotification);
        return notification;
    }

    @Override
    public void sendEventCreatedNotification(Event event, Host host) {
        Notification notification = new Notification();
        UUID orgManagerId = host.getCreatedBy().getId();

        notification.setType(ENotificationType.MNG_EVENT_CREATED);
        notification.setTitle("Sự kiện mới được tạo");
        notification.setBody(String.format("Sự kiện \"%s\" vừa được tạo và cần xác nhận.", event.getName()));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_CREATED.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));

        notification = saveNotificationForUser(notification, orgManagerId);

        notificationPublisher.enqueueNotification(notification, orgManagerId);
    }

    @Override
    @Transactional
    public void sendEventApprovedByOrgManagerNotification(Event event) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện đã được Quản lí tổ chức phê duyệt");
        notificationForHost.setBody(String.format("Sự kiện %s đã được phê duyệt bởi quản lí tổ chức và đang chờ duyệt từ Admin.", event.getName()));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_APPROVED_BY_MNG);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to admin
        Notification notificationForAdmin = new Notification();
        notificationForAdmin.setTopic(ADMIN_TOPIC);
        notificationForAdmin.setTitle("Sự kiện đã được Quản lí tổ chức phê duyệt");
        notificationForAdmin.setBody(String.format("Sự kiện %s đã được phê duyệt bởi quản lí tổ chức và cần bạn xác nhận.", event.getName()));
        notificationForAdmin.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.ADM_EVENT_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.ADM_EVENT_DETAILS.name()
        ));
        notificationForAdmin.setType(ENotificationType.ADM_EVENT_APPROVED_BY_MNG);
        notificationRepository.save(notificationForAdmin);

        //send notification
        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForAdmin, null);
    }

    @Override
    public void sendEventRejectedByOrgManagerNotification(Event event, String reason) {
        //send notification to host
        Notification notification = new Notification();
        UUID hostId = event.getHost().getId();

        notification.setTitle("Sự kiện không được chấp thuận bởi Quản lí tổ chức");
        notification.setBody(String.format("Quản lí tổ chức đã không chấp thuận tạo sự kiện %s. Lí do: %s", event.getName(), reason));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_REJECTED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.HOST_EVENT_REJECTED_BY_MNG);

        //save notification
        notification = saveNotificationForUser(notification, hostId);

        notificationPublisher.enqueueNotification(notification, hostId);
    }

    @Override
    public void sendEventApprovedByAdminNotification(Event event) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện đã được Admin phê duyệt");
        notificationForHost.setBody(String.format("Sự kiện %s đã được phê duyệt bởi Admin và bước vào tranng thái tuyển người.", event.getName()));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_APPROVED_BY_AD.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_APPROVED_BY_AD);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to manager
        Notification notificationForManager = new Notification();
        UUID orgManagerId = event.getHost().getCreatedBy().getId();
        notificationForManager.setTitle("Sự kiện đã được Admin phê duyệt");
        notificationForManager.setBody(String.format("Sự kiện %s đã được phê duyệt bởi Admin và bước vào tranng thái tuyển người.", event.getName()));
        notificationForManager.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_APPROVED_BY_AD.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notificationForManager.setType(ENotificationType.MNG_EVENT_APPROVED_BY_AD);

        //save notification
        notificationForManager = saveNotificationForUser(notificationForManager, orgManagerId);


        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForManager, orgManagerId);
    }

    @Override
    public void sendEventRejectedByAdminNotification(Event event, String reason) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện không được chấp thuận bởi Admin");
        notificationForHost.setBody(String.format("Sự kiện %s đã không được chấp thuận bởi admin với lí do: %s.", event.getName(), reason));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_REJECTED_BY_AD.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_REJECTED_BY_AD);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to manager
        Notification notificationForManager = new Notification();
        UUID orgManagerId = event.getHost().getCreatedBy().getId();

        notificationForManager.setTitle("Sự kiện không được chấp thuận bởi Admin");
        notificationForManager.setBody(String.format("Sự kiện %s đã không được chấp thuận bởi admin với lí do: %s.", event.getName(), reason));
        notificationForManager.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_REJECTED_BY_AD.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notificationForManager.setType(ENotificationType.MNG_EVENT_REJECTED_BY_AD);

        //save notification
        notificationForManager = saveNotificationForUser(notificationForManager, orgManagerId);

        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForManager, orgManagerId);
    }

    @Override
    public void sendEventApplicationApproved(UUID volunteerId, Event event, EventApplication application) {
        //send notification to host
        Notification notification = new Notification();

        notification.setTitle("Đơn đăng kí tham gia sự kiện tình nguyện đã được phê duyệt");
        notification.setBody(String.format("Quản lí sự kiện %s đã phê duyệt đơn đăng kí tham gia tình nguyện ngày %s của bạn.", event.getName(), application.getSessionDate()));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_APPLICATION_APPROVED.name(),
                DATA_REF_ID_KEY, application.getId().toString(),
                DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_APPLICATION_APPROVED);

        //save notification
        notification = saveNotificationForUser(notification, volunteerId);

        notificationPublisher.enqueueNotification(notification, volunteerId);
    }

    @Override
    public void sendEventApplicationRejected(UUID volunteerId, Event event, EventApplication application, String rejectionReason) {
        //send notification to volunteer
        Notification notification = new Notification();

        notification.setTitle("Đơn đăng kí tham gia sự kiện tình nguyện không được chấp thuận");
        notification.setBody(String.format("Quản lí sự kiện %s đã không chấp thuận đơn đăng kí tham gia tình nguyện ngày %s với lí do: %s",
                event.getName(),
                application.getSessionDate(),
                rejectionReason)
        );
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_APPLICATION_REJECTED.name(),
                DATA_REF_ID_KEY, application.getId().toString(),
                DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_APPLICATION_REJECTED);

        //save notification
        notification = saveNotificationForUser(notification, volunteerId);

        notificationPublisher.enqueueNotification(notification, volunteerId);
    }

    @Override
    public void sendEventApplicationCancelledSuccessfully(UUID volunteerId, Event event, EventApplication application, boolean isMinusScore) {
        //send notification to volunteer
        Notification notification = new Notification();

        notification.setTitle("Đơn đăng kí tham gia sự kiện tình nguyện đã được hủy thành công");
        String body = String.format("Bạn đã hủy đơn đăng kí tham gia sự kiện %s ngày %s thành công.",
                event.getName(),
                application.getSessionDate()
        );

        if (isMinusScore) {
            body = body.concat(" Tuy nhiên do thời gian tuyển người của sự kiện đã kết thúc và bạn đã hủy tham gia sự kiện trước thời gian diễn ra, nên chúng tôi sẽ trừ 3 điểm trong Điểm vinh dự của bạn.");
        }
        notification.setBody(body);
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_APPLICATION_CANCELLED.name(),
                DATA_REF_ID_KEY, application.getId().toString(),
                DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_APPLICATION_CANCELLED);

        //save notification
        notification = saveNotificationForUser(notification, volunteerId);

        notificationPublisher.enqueueNotification(notification, volunteerId);
    }

    @Override
    public void sendNotificationToVolunteersOfEvent(UUID eventId, AnnounceVolunteerRequest request) {
        String eventTopicName = EVENT_TOPIC_PRE + eventId;

        //send notification to topic
        Notification notificationForVolunteers = new Notification();
        notificationForVolunteers.setTopic(eventTopicName);
        notificationForVolunteers.setTitle(request.getTitle());
        notificationForVolunteers.setBody(request.getBody());
        notificationForVolunteers.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_EVENT_ANNOUNCEMENT.name()
        ));
        notificationForVolunteers.setType(ENotificationType.VOL_EVENT_ANNOUNCEMENT);
        notificationRepository.save(notificationForVolunteers);

        //send notification to topic
        notificationPublisher.enqueueNotification(notificationForVolunteers, null);
    }


}
