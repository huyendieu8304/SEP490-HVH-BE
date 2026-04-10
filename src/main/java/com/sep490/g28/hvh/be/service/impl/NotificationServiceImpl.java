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
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final UserRepository userRepository;
    private final NotificationTokenRepository notificationTokenRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationTopicSubscriptionRepository notificationTopicSubscriptionRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final EventApplicationRepository eventApplicationRepository;


    private final CurrentUserProvider currentUserProvider;

    private final NotificationTokenTxService notificationTokenTxService;

    private final NotificationPublisher notificationPublisher;

    private static final String ORG_TOPIC_PRE = "org_"; //org_{orgId}
    private static final String EVENT_TOPIC_PRE = "event_"; //event_{eventId}
    private static final String ADMIN_TOPIC = "admin";

    private static final String DATA_REF_ID_KEY = "refId";
    private static final String DATA_ACTION = "action";
    private static final String DATA_NOTIFICATION_TYPE = "type";
    private final EventSessionRepository eventSessionRepository;

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

        notification.setTitle("Sự kiện mới được tạo");
        notification.setBody(String.format("Sự kiện \"%s\" vừa được tạo và cần xác nhận.", event.getName()));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_CREATED.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.MNG_EVENT_CREATED);

        notification = saveNotificationForUser(notification, orgManagerId);

        notificationPublisher.enqueueNotification(notification, orgManagerId);
    }

    @Override
    public void sendEventCreateApprovedByOrgManagerNotification(Event event) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện đã được Quản lí tổ chức phê duyệt");
        notificationForHost.setBody(String.format("Sự kiện %s đã được phê duyệt bởi quản lí tổ chức và đang chờ duyệt từ Admin.", event.getName()));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_CREATE_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_CREATE_APPROVED_BY_MNG);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to admin
        Notification notificationForAdmin = new Notification();
        notificationForAdmin.setTopic(ADMIN_TOPIC);
        notificationForAdmin.setTitle("Sự kiện đã được Quản lí tổ chức phê duyệt");
        notificationForAdmin.setBody(String.format("Sự kiện %s đã được phê duyệt bởi quản lí tổ chức và cần bạn xác nhận.", event.getName()));
        notificationForAdmin.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.ADM_EVENT_CREATE_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.ADM_EVENT_DETAILS.name()
        ));
        notificationForAdmin.setType(ENotificationType.ADM_EVENT_CREATE_APPROVED_BY_MNG);
        notificationRepository.save(notificationForAdmin);

        //send notification
        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForAdmin, null);
    }

    @Override
    public void sendEventCreateRejectedByOrgManagerNotification(Event event, String reason) {
        //send notification to host
        Notification notification = new Notification();
        UUID hostId = event.getHost().getId();

        notification.setTitle("Sự kiện không được chấp thuận bởi Quản lí tổ chức");
        notification.setBody(String.format("Quản lí tổ chức đã không chấp thuận tạo sự kiện %s. Lí do: %s", event.getName(), reason));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_CREATE_REJECTED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.HOST_EVENT_CREATE_REJECTED_BY_MNG);

        //save notification
        notification = saveNotificationForUser(notification, hostId);

        notificationPublisher.enqueueNotification(notification, hostId);
    }

    @Override
    public void sendEventCreateApprovedByAdminNotification(Event event) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện đã được Admin phê duyệt");
        notificationForHost.setBody(String.format("Sự kiện %s đã được phê duyệt bởi Admin và bước vào tranng thái tuyển người.", event.getName()));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_CREATE_APPROVED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_CREATE_APPROVED_BY_ADM);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to manager
        Notification notificationForManager = new Notification();
        UUID orgManagerId = event.getHost().getCreatedBy().getId();
        notificationForManager.setTitle("Sự kiện đã được Admin phê duyệt");
        notificationForManager.setBody(String.format("Sự kiện %s đã được phê duyệt bởi Admin và bước vào tranng thái tuyển người.", event.getName()));
        notificationForManager.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_CREATE_APPROVED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notificationForManager.setType(ENotificationType.MNG_EVENT_CREATE_APPROVED_BY_ADM);

        //save notification
        notificationForManager = saveNotificationForUser(notificationForManager, orgManagerId);


        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForManager, orgManagerId);
    }

    @Override
    public void sendEventCreateRejectedByAdminNotification(Event event, String reason) {
        //send notification to host
        Notification notificationForHost = new Notification();
        UUID hostId = event.getHost().getId();

        notificationForHost.setTitle("Sự kiện không được chấp thuận bởi Admin");
        notificationForHost.setBody(String.format("Sự kiện %s đã không được chấp thuận bởi admin với lí do: %s.", event.getName(), reason));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_CREATE_REJECTED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_CREATE_REJECTED_BY_ADM);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to manager
        Notification notificationForManager = new Notification();
        UUID orgManagerId = event.getHost().getCreatedBy().getId();

        notificationForManager.setTitle("Sự kiện không được chấp thuận bởi Admin");
        notificationForManager.setBody(String.format("Sự kiện %s đã không được chấp thuận bởi admin với lí do: %s.", event.getName(), reason));
        notificationForManager.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_CREATE_REJECTED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notificationForManager.setType(ENotificationType.MNG_EVENT_CREATE_REJECTED_BY_ADM);

        //save notification
        notificationForManager = saveNotificationForUser(notificationForManager, orgManagerId);

        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForManager, orgManagerId);
    }

    @Override
    public void sendEventApplicationApprovedNotification(UUID volunteerId, Event event, EventApplication application) {
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
    public void sendEventApplicationRejectedNotification(UUID volunteerId, Event event, EventApplication application, String rejectionReason) {
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
    public void sendEventApplicationCancelledSuccessfullyNotification(UUID volunteerId, Event event, EventApplication application, boolean isMinusScore) {
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

    @Override
    public void sentEventCancelledByHostNotification(List<EventApplication> eventApplications, String eventName, String cancelReason) {

        //ListNotification
        List<Notification> notifications = new ArrayList<>();
        for (EventApplication application : eventApplications) {
            Notification notification = buildEventCancelledByHostNotification(eventName, cancelReason, application);
            notifications.add(notification);
        }
        notifications = notificationRepository.saveAll(notifications);

        pushNotificationsToMessageQueue(eventApplications, notifications);
    }

    private Notification buildEventCancelledByHostNotification(String eventName, String cancelReason, EventApplication application) {
        Notification notification = new Notification();
        notification.setTitle("Sự kiện đã bị hủy bởi Host");
        notification.setBody(String.format(
                "Sự kiện %s đã bị Host hủy và không tiếp tục diễn ra với lí do: %s. " +
                        "Đơn đăng kí tham gia sự kiện vào ngày %s của bạn sẽ được tự động hủy và sẽ không ảnh hưởng đến số điểm hiện tại bạn đang có.",
                eventName, cancelReason, application.getSessionDate().toString()
        ));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_EVENT_CANCELLED_BY_HOST.name(),
                DATA_REF_ID_KEY, application.getId().toString(),
                DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_EVENT_CANCELLED_BY_HOST);
        return notification;
    }

    @Override
    public void sentEventCancelledByAdminNotification(List<EventApplication> eventApplications, String eventName, String cancelReason) {

        //ListNotification
        List<Notification> notifications = new ArrayList<>();
        for (EventApplication application : eventApplications) {
            Notification notification = buildEventCancelledByAdminNotification(eventName, cancelReason, application);
            notifications.add(notification);
        }
        notifications = notificationRepository.saveAll(notifications);

        pushNotificationsToMessageQueue(eventApplications, notifications);
    }

    private Notification buildEventCancelledByAdminNotification(String eventName, String cancelReason, EventApplication application) {
        Notification notification = new Notification();
        notification.setTitle("Sự kiện đã bị hủy bởi quản trị viên hệ thống");
        notification.setBody(String.format(
                "Sự kiện %s đã bị quản trị viên hệ thống hủy và không tiếp tục diễn ra với lí do: %s. " +
                        "Đơn đăng kí tham gia sự kiện vào ngày %s của bạn sẽ được tự động hủy và sẽ không ảnh hưởng đến số điểm hiện tại bạn đang có.",
                eventName, cancelReason, application.getSessionDate().toString()
        ));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_EVENT_CANCELLED_BY_AD.name(),
                DATA_REF_ID_KEY, application.getId().toString(),
                DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_EVENT_CANCELLED_BY_AD);
        return notification;
    }

    private void pushNotificationsToMessageQueue(List<EventApplication> eventApplications, List<Notification> notifications) {
        //link volunteer to notification
        List<UserNotification> userNotifications = new ArrayList<>();
        for (int i = 0; i < notifications.size(); i++) {
            EventApplication app = eventApplications.get(i);
            Notification noti = notifications.get(i);

            UserNotification un = new UserNotification();
            un.setNotification(noti);
            un.setUser(userRepository.getReferenceById(app.getVolunteer().getId()));

            userNotifications.add(un);
        }
        userNotificationRepository.saveAll(userNotifications);

        //push notification
        for (int i = 0; i < notifications.size(); i++) {
            notificationPublisher.enqueueNotification(
                    notifications.get(i),
                    eventApplications.get(i).getVolunteer().getId()
            );
        }
    }


    @Override
    public void sentEventUpdatedByHostNotification(UUID orgManagerId, UUID eventId, String eventName) {
        Notification notification = new Notification();

        notification.setType(ENotificationType.MNG_EVENT_UPDATED_BY_HOST);
        notification.setTitle("Sự kiện được cập nhật thông tin");
        notification.setBody(String.format("Sự kiện \"%s\" vừa được cập nhật thông tin và cần xác nhận.",eventName));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_UPDATED_BY_HOST.name(),
                DATA_REF_ID_KEY, eventId.toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));

        notification = saveNotificationForUser(notification, orgManagerId);

        notificationPublisher.enqueueNotification(notification, orgManagerId);
    }

    @Override
    public void sendEventUpdateNonCriticalApprovedByOrgManagerNotification(UUID hostId, Event event) {
        //send notification to host
        Notification notificationForHost = new Notification();

        notificationForHost.setTitle("Cập nhật sự kiện đã được quản lí tổ chức phê duyệt");
        notificationForHost.setBody(String.format("Sự kiện %s đã được phê duyệt cập nhật bởi quản lí tổ chức.", event.getName()));
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UPDATE_NON_CRITICAL_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_UPDATE_NON_CRITICAL_APPROVED_BY_MNG);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        notificationPublisher.enqueueNotification(notificationForHost, hostId);
    }

    @Override
    public void sendEventUpdateNonCriticalApprovedByOrgManagerNotification(List<EventApplication> eventApplications, String eventName) {
        List<Notification> notifications = new ArrayList<>();

        for (EventApplication app : eventApplications) {
            Notification notification = new Notification();
            notification.setTitle("Cập nhật thông tin sự kiện");
            notification.setBody(String.format(
                    "Sự kiện %s đã được cập nhật một số thông tin, bạn hãy kiểm lại để đảm bảo nắm được thông tin thay đổi.",
                    eventName
            ));
            notification.setData(Map.of(
                    DATA_NOTIFICATION_TYPE, ENotificationType.VOL_EVENT_UPDATE_NON_CRITICAL_APPROVED_BY_MNG.name(),
                    DATA_REF_ID_KEY, app.getId().toString(),
                    DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
            ));
            notification.setType(ENotificationType.VOL_EVENT_UPDATE_NON_CRITICAL_APPROVED_BY_MNG);

            notifications.add(notification);
        }

        notifications = notificationRepository.saveAll(notifications);
        pushNotificationsToMessageQueue(eventApplications, notifications);
    }

    @Override
    public void sendEventUpdateCriticalApprovedByOrgManagerNotification(UUID hostId, Event event) {
        Notification notification = new Notification();

        notification.setTitle("Cập nhật quan trọng đã được phê duyệt");
        notification.setBody(String.format(
                "Cập nhật quan trọng của sự kiện %s đã được quản lí tổ chức phê duyệt và đang chờ Admin.",
                event.getName()
        ));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UPDATE_CRITICAL_APPROVED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.HOST_EVENT_UPDATE_CRITICAL_APPROVED_BY_MNG);

        notification = saveNotificationForUser(notification, hostId);
        notificationPublisher.enqueueNotification(notification, hostId);
    }

    @Override
    public void sendEventUpdateRejectedByOrgManagerNotification(UUID hostId, Event event) {
        Notification notification = new Notification();

        notification.setTitle("Yêu cầu cập nhật bị quản lí tổ chức từ chối");
        notification.setBody(String.format(
                "Yêu cầu cập nhật sự kiện %s đã bị quản lí tổ chức từ chối.",
                event.getName()
        ));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UPDATE_REJECTED_BY_MNG.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.HOST_EVENT_UPDATE_REJECTED_BY_MNG);

        notification = saveNotificationForUser(notification, hostId);
        notificationPublisher.enqueueNotification(notification, hostId);
    }

    @Override
    public void sendEventUpdateCriticalApprovedByAdminNotification(Event event) {
        // host
        UUID hostId = event.getHost().getId();

        Notification notiHost = new Notification();
        notiHost.setTitle("Yêu cầu cập nhật quan trọng đã được Admin phê duyệt");
        notiHost.setBody(String.format(
                "Yêu cầu cập nhật quan trọng của sự kiện %s đã được Admin phê duyệt.",
                event.getName()
        ));
        notiHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notiHost.setType(ENotificationType.HOST_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM);

        notiHost = saveNotificationForUser(notiHost, hostId);

        // manager
        UUID managerId = event.getOrganization().getOrganizationManager().getId();

        Notification notiMng = new Notification();
        notiMng.setTitle("Yêu cầu cập nhật quan trọng đã được Admin phê duyệt");
        notiMng.setBody(String.format(
                "Yêu cầu cập nhật quan trọng của sự kiện %s đã được Admin phê duyệt.",
                event.getName()
        ));
        notiMng.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notiMng.setType(ENotificationType.MNG_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM);

        notiMng = saveNotificationForUser(notiMng, managerId);

        notificationPublisher.enqueueNotification(notiHost, hostId);
        notificationPublisher.enqueueNotification(notiMng, managerId);
    }

    @Override
    public void sendEventUpdateCriticalApprovedByAdminNotification(List<EventApplication> eventApplications, String eventName) {
        List<Notification> notifications = new ArrayList<>();

        for (EventApplication app : eventApplications) {
            Notification notification = new Notification();
            notification.setTitle("Cập nhật sự kiện");
            notification.setBody(String.format(
                    "Sự kiện %s đã được cập nhật một số thông tin quan trọng, " +
                            "do đó, đơn đăng kí buổi tình nguyện ngày %s của bạn đã bị hủy, " +
                            "hãy đăng kí lại nếu bạn vẫn muốn tham gia sự kiện.",
                    eventName,
                    app.getSessionDate()
            ));
            notification.setData(Map.of(
                    DATA_NOTIFICATION_TYPE, ENotificationType.VOL_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM.name(),
                    DATA_REF_ID_KEY, app.getId().toString(),
                    DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
            ));
            notification.setType(ENotificationType.VOL_EVENT_UPDATE_CRITICAL_APPROVED_BY_ADM);

            notifications.add(notification);
        }

        notifications = notificationRepository.saveAll(notifications);
        pushNotificationsToMessageQueue(eventApplications, notifications);
    }

    @Override
    public void sendEventUpdateCriticalRejectedByAdminNotification(Event event) {
        UUID hostId = event.getHost().getId();

        Notification notiHost = new Notification();
        notiHost.setTitle("Yêu cầu cập nhật quan trọng bị Admin từ chối");
        notiHost.setBody(String.format(
                "Yêu cầu cập nhật quan trọng của sự kiện %s đã bị Admin từ chối.",
                event.getName()
        ));
        notiHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UPDATE_CRITICAL_REJECTED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notiHost.setType(ENotificationType.HOST_EVENT_UPDATE_CRITICAL_REJECTED_BY_ADM);

        notiHost = saveNotificationForUser(notiHost, hostId);

        // manager
        UUID managerId = event.getOrganization().getOrganizationManager().getId();

        Notification notiMng = new Notification();
        notiMng.setTitle("Yêu cầu cập nhật quan trọng bị Admin từ chối");
        notiMng.setBody(String.format(
                "Yêu cầu cập nhật quan trọng của sự kiện %s đã bị Admin từ chối.",
                event.getName()
        ));
        notiMng.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_UPDATE_CRITICAL_REJECTED_BY_ADM.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notiMng.setType(ENotificationType.MNG_EVENT_UPDATE_CRITICAL_REJECTED_BY_ADM);

        notiMng = saveNotificationForUser(notiMng, managerId);

        notificationPublisher.enqueueNotification(notiHost, hostId);
        notificationPublisher.enqueueNotification(notiMng, managerId);
    }

    @Override
    public void sendEventAssignedHostNotification(UUID oldHostId, UUID newHostId, Event event) {
        Notification notiOldHost = new Notification();
        notiOldHost.setTitle("Phân công sự kiện");
        notiOldHost.setBody(String.format(
                "Sự kiện %s đã được quản lí tổ chức phân công cho host khác.",
                event.getName()
        ));
        notiOldHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_UNASSIGNED.name()
        ));
        notiOldHost.setType(ENotificationType.HOST_EVENT_UNASSIGNED);

        notiOldHost = saveNotificationForUser(notiOldHost, oldHostId);

        Notification notiNewHost = new Notification();
        notiNewHost.setTitle("Bạn được phân công sự kiện");
        notiNewHost.setBody(String.format(
                "Sự kiện %s đã được quản lí tổ chức phân công cho bạn.",
                event.getName()
        ));
        notiNewHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_ASSIGNED.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notiNewHost.setType(ENotificationType.HOST_EVENT_ASSIGNED);

        notiNewHost = saveNotificationForUser(notiNewHost, oldHostId);

        notificationPublisher.enqueueNotification(notiOldHost, oldHostId);
        notificationPublisher.enqueueNotification(notiNewHost, newHostId);

    }

    @Override
    public void sendVolunteerReviewedByHostNotification(UUID volunteerId, Event event, EventApplication application, UUID reviewId) {
        Notification notification = new Notification();

        notification.setTitle("Đánh giá quá trình tham gia sự kiện");
        notification.setBody(String.format(
                "Bạn đã được đánh giá quá trình tham gia sự kiện %s ngày %s bởi người tổ chức.",
                event.getName(),
                application.getSessionDate().toString()
        ));
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_REVIEWED_BY_HOST.name(),
                DATA_REF_ID_KEY, reviewId.toString(),
                DATA_ACTION, ENotificationDataAction.VOL_REVIEW_DETAILS.name()
        ));
        notification.setType(ENotificationType.VOL_REVIEWED_BY_HOST);

        notification = saveNotificationForUser(notification, volunteerId);
        notificationPublisher.enqueueNotification(notification, volunteerId);
    }

    @Override
    public void sendVolunteerReceivedCertificateNotification(Volunteer volunteer, Event event) {
        Notification notification = buildVolunteerReceivedCertificateNotification(event.getName());

        notification = saveNotificationForUser(notification, volunteer.getId());
        notificationPublisher.enqueueNotification(notification, volunteer.getId());
    }

    private Notification buildVolunteerReceivedCertificateNotification(String eventName) {
        Notification notification = new Notification();
        notification.setTitle("Nhận được chứng chỉ");
        notification.setBody(String.format(
                "Xin chúc mừng bạn đã nhận được chứng chỉ chứng nhận những đóng góp của bạn cho sự kiện %s.",
                eventName
        ));

        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.VOL_RECEIVED_CERTIFICATE.name(),
                DATA_ACTION, ENotificationDataAction.VOL_CERTIFICATES.name()
        ));
        notification.setType(ENotificationType.VOL_RECEIVED_CERTIFICATE);
        return notification;
    }

    @Override
    public void sendVolunteersReceivedCertificatesNotifications(List<Volunteer> volunteers, Event event) {

        Notification notification = buildVolunteerReceivedCertificateNotification(event.getName());
        notification = notificationRepository.save(notification);

        //link volunteer to notification
        List<UserNotification> userNotifications = new ArrayList<>();
        for (Volunteer value : volunteers) {
            UserNotification un = new UserNotification();
            un.setNotification(notification);
            un.setUser(userRepository.getReferenceById(value.getId()));

            userNotifications.add(un);
        }
        userNotificationRepository.saveAll(userNotifications);

        //push notification
        for (Volunteer volunteer : volunteers) {
            notificationPublisher.enqueueNotification(
                    notification,
                    volunteer.getId()
            );
        }
//        log.info("Send volunteer received certificates notification for {} participated in event {}", volunteers.size(), event.getName());
    }

    @Override
    public void sendEventCompletedNotifications(Event event, UUID orgManagerId, UUID hostId) {
        //send notification to host
        Notification notificationForHost = new Notification();

        notificationForHost.setTitle("Sự kiện đã hoàn thành");
        notificationForHost.setBody(String.format("Sự kiện %s đã qua 2 ngày kể từ ngày kết thúc sự kiện," +
                " những tình nguyện viên đủ điều kiện đã được đánh giá tự động 5 sao cho tất cả các tiêu chí" +
                " và chứng chỉ đã được gửi tới những tình nguyện viên hợp lệ. Chúc mừng bạn đã hoàn thành sự kiện. " +
                "Chúng tôi rất mong sẽ được đồng hành cùng bạn trong các sự kiện sắp tới.",
                event.getName())
        );
        notificationForHost.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_COMPLETED.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notificationForHost.setType(ENotificationType.HOST_EVENT_COMPLETED);

        //save notification
        notificationForHost = saveNotificationForUser(notificationForHost, hostId);

        //send notification to manager
        Notification notificationForManager = new Notification();
        notificationForManager.setTitle("Sự kiện đã hoàn thành");
        notificationForManager.setBody(String.format("Sự kiện %s đã qua 2 ngày kể từ ngày kết thúc sự kiện," +
                " những tình nguyện viên đủ điều kiện đã được đánh giá tự động 5 sao cho tất cả các tiêu chí" +
                " và chứng chỉ đã được gửi tới những tình nguyện viên hợp lệ. Chúc mừng bạn đã hoàn thành sự kiện. " +
                "Chúng tôi rất mong sẽ được đồng hành cùng bạn trong các sự kiện sắp tới.",
                event.getName())
        );
        notificationForManager.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.MNG_EVENT_COMPLETED.name(),
                DATA_REF_ID_KEY, event.getId().toString(),
                DATA_ACTION, ENotificationDataAction.MNG_EVENT_DETAILS.name()
        ));
        notificationForManager.setType(ENotificationType.MNG_EVENT_COMPLETED);

        //save notification
        notificationForManager = saveNotificationForUser(notificationForManager, orgManagerId);

        notificationPublisher.enqueueNotification(notificationForHost, hostId);
        notificationPublisher.enqueueNotification(notificationForManager, orgManagerId);
    }

    @Override
    public void sendCheckInCodeOfEventSessionNotifications(
            List<EventApplication> eventApplications,
            String eventName,
            String checkInCode) {
        List<Notification> notifications = new ArrayList<>();
        for (EventApplication app : eventApplications) {
            Notification notification = new Notification();
            notification.setTitle(String.format("Check in code sự kiện %s",eventName));
            notification.setBody(String.format("Hôm nay là ngày diễn ra session mà bạn đã đăng kí của sự kiện %s, để check in sự kiện, hãy nhập code sau: %s",
                            eventName,
                            checkInCode
                    )
            );
            notification.setData(Map.of(
                    DATA_NOTIFICATION_TYPE, ENotificationType.VOL_CHECK_IN_CODE.name(),
                    DATA_REF_ID_KEY, app.getId().toString(),
                    DATA_ACTION, ENotificationDataAction.VOL_APPLICATION_DETAILS.name()
            ));
            notification.setType(ENotificationType.VOL_CHECK_IN_CODE);

            notifications.add(notification);
        }
        notifications = notificationRepository.saveAll(notifications);
        pushNotificationsToMessageQueue(eventApplications, notifications);
    }

    @Override
    public void sendEventSessionHostedTodayNotification(UUID hostId, UUID eventId, String eventName){
        Notification notification = new Notification();
        notification.setTitle(String.format("Hôm nay bạn host sự kiện %s",eventName));
        notification.setBody(String.format("Hôm nay là ngày diễn ra session của sự kiện %s, chúc sự kiện của bạn diễn ra suôn sẻ!",
                        eventName
                )
        );
        notification.setData(Map.of(
                DATA_NOTIFICATION_TYPE, ENotificationType.HOST_EVENT_SESSION_TODAY.name(),
                DATA_REF_ID_KEY, eventId.toString(),
                DATA_ACTION, ENotificationDataAction.HOST_EVENT_DETAILS.name()
        ));
        notification.setType(ENotificationType.HOST_EVENT_SESSION_TODAY);

        notificationRepository.save(notification);

        notification = saveNotificationForUser(notification, hostId);
        notificationPublisher.enqueueNotification(notification, hostId);
    }
}
