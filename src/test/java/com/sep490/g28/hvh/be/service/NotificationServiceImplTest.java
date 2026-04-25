package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EPlatform;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.dto.notification.request.RegisterNotificationTokenRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.notification.entity.Notification;
import com.sep490.g28.hvh.be.notification.entity.NotificationToken;
import com.sep490.g28.hvh.be.notification.messageque.NotificationPublisher;
import com.sep490.g28.hvh.be.notification.repository.NotificationRepository;
import com.sep490.g28.hvh.be.notification.repository.NotificationTokenRepository;
import com.sep490.g28.hvh.be.notification.repository.NotificationTopicSubscriptionRepository;
import com.sep490.g28.hvh.be.notification.repository.UserNotificationRepository;
import com.sep490.g28.hvh.be.notification.service.NotificationTokenTxService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationTokenRepository notificationTokenRepository;

    @Mock
    private NotificationTopicSubscriptionRepository notificationTopicSubscriptionRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private NotificationTokenTxService notificationTokenTxService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    UserNotificationRepository userNotificationRepository;



    private UUID userId;

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
    }

    private RegisterNotificationTokenRequest validReq() {
        RegisterNotificationTokenRequest r = new RegisterNotificationTokenRequest();
        ReflectionTestUtils.setField(r, "token", "abcdefghijklmnopqrstuvwxyz123456");
        ReflectionTestUtils.setField(r, "deviceId", "device_abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(r, "platform", EPlatform.ANDROID);
        return r;
    }

    // ====================registerNotificationToken
    @Test
    void registerNotificationToken_success() {
        RegisterNotificationTokenRequest req = validReq();

        User user = new User();
        user.setId(userId);
        when(currentUserProvider.getId()).thenReturn(userId);


        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationTokenRepository.findByUserIdAndPlatformAndDeviceId(any(), any(), any()))
                .thenReturn(Optional.empty());

        when(notificationTopicSubscriptionRepository.findTopicsByUserId(userId))
                .thenReturn(new ArrayList<>(List.of("topic1")));

        when(currentUserProvider.getRoleName()).thenReturn(ERole.VOL);

        service.registerNotificationToken(req);

        verify(notificationTokenRepository).save(any());
        verify(notificationPublisher).enqueueSubscribeToTopics(eq(req.getToken()), anyList());
    }

    @Test
    void registerNotificationToken_admin_shouldAddAdminTopic() {
        RegisterNotificationTokenRequest req = validReq();

        User user = new User(); user.setId(userId);
        when(currentUserProvider.getId()).thenReturn(userId);


        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationTokenRepository.findByUserIdAndPlatformAndDeviceId(any(), any(), any()))
                .thenReturn(Optional.empty());

        when(notificationTopicSubscriptionRepository.findTopicsByUserId(userId))
                .thenReturn(new ArrayList<>());

        when(currentUserProvider.getRoleName()).thenReturn(ERole.SYS_ADMIN);

        service.registerNotificationToken(req);

        verify(notificationPublisher).enqueueSubscribeToTopics(
                eq(req.getToken()),
                argThat(list -> list.contains("admin"))
        );
    }

    @Test
    void registerNotificationToken_existingToken_shouldUpdate() {
        RegisterNotificationTokenRequest req = validReq();

        User user = new User(); user.setId(userId);
        when(currentUserProvider.getId()).thenReturn(userId);

        NotificationToken existing = new NotificationToken();

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(notificationTokenRepository.findByUserIdAndPlatformAndDeviceId(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        when(notificationTopicSubscriptionRepository.findTopicsByUserId(userId))
                .thenReturn(new ArrayList<>());

        when(currentUserProvider.getRoleName()).thenReturn(ERole.VOL);

        service.registerNotificationToken(req);

        assertEquals(req.getToken(), existing.getToken());
        verify(notificationTokenRepository).save(existing);
    }

    // ========== unregisterNotificationToken ==============
    @Test
    void unregisterNotificationToken_success() {
        String token = "abc123";

        when(currentUserProvider.getId()).thenReturn(userId);

        when(notificationTopicSubscriptionRepository.findTopicsByUserId(userId))
                .thenReturn(new ArrayList<>(List.of("topic1")));

        when(currentUserProvider.getRoleName()).thenReturn(ERole.VOL);

        service.unregisterNotificationToken(token);

        verify(notificationTokenTxService).deleteToken(token);
        verify(notificationPublisher).enqueueUnsubscribeFromTopics(eq(token), anyList());
    }

    @Test
    void unregisterNotificationToken_admin_shouldIncludeAdminTopic() {
        String token = "abc123";

        when(currentUserProvider.getId()).thenReturn(userId);

        when(notificationTopicSubscriptionRepository.findTopicsByUserId(userId))
                .thenReturn(new ArrayList<>());

        when(currentUserProvider.getRoleName()).thenReturn(ERole.SYS_ADMIN);

        service.unregisterNotificationToken(token);

        verify(notificationPublisher).enqueueUnsubscribeFromTopics(
                eq(token),
                argThat(list -> list.contains("admin"))
        );
    }

    // ========== subscribeUserToTopicOfEvent =====================
    @Test
    void subscribeUserToTopicOfEvent_success() {
        UUID eventId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        when(userRepository.getReferenceById(userId)).thenReturn(user);

        service.subscribeUserToTopicOfEvent(userId, eventId);

        verify(notificationTopicSubscriptionRepository).save(argThat(sub ->
                sub.getTopic().equals("event_" + eventId)
        ));

        verify(notificationPublisher)
                .enqueueSubscribeUserToTopic(userId, "event_" + eventId);
    }

    // ======================== unsubscribeUserFromTopicOfEvent
    @Test
    void unsubscribeUserFromTopicOfEvent_success() {
        UUID eventId = UUID.randomUUID();

        service.unsubscribeUserFromTopicOfEvent(userId, eventId);

        verify(notificationTopicSubscriptionRepository)
                .deleteByUser_IdAndTopic(userId, "event_" + eventId);

        verify(notificationPublisher)
                .enqueueUnsubscribeUserFromTopic(userId, "event_" + eventId);
    }

    @Test
    void sendEventCreatedNotification_success() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");

        OrganizationManager creator = new OrganizationManager();
        creator.setId(userId);

        Host host = new Host();
        host.setCreatedBy(creator);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventCreatedNotification(event, host);

        verify(notificationRepository).save(any(Notification.class));
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(userId));
    }

    @Test
    void sendEventCreateApprovedByOrgManagerNotification_success() {
        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");
        event.setHost(host);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventCreateApprovedByOrgManagerNotification(event);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
        verify(notificationPublisher).enqueueNotification(any(), isNull());
    }
    @Test
    void sendEventCreateRejectedByOrgManagerNotification_success() {
        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");
        event.setHost(host);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventCreateRejectedByOrgManagerNotification(event, "reason");

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
    }

    @Test
    void sendEventCreateApprovedByAdminNotification_success() {
        UUID hostId = UUID.randomUUID();
        UUID mngId = UUID.randomUUID();

        OrganizationManager creator = new OrganizationManager();
        creator.setId(mngId);

        Host host = new Host();
        host.setId(hostId);
        host.setCreatedBy(creator);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");
        event.setHost(host);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventCreateApprovedByAdminNotification(event);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository, times(2)).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
        verify(notificationPublisher).enqueueNotification(any(), eq(mngId));
    }

    @Test
    void sendEventApplicationApprovedNotification_success() {
        UUID volId = UUID.randomUUID();

        Event event = new Event();
        event.setName("event");

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventApplicationApprovedNotification(volId, event, app);

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendEventApplicationCancelledSuccessfully_withMinusScore() {
        UUID volId = UUID.randomUUID();

        Event event = new Event();
        event.setName("event");

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventApplicationCancelledSuccessfullyNotification(volId, event, app, true);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        assertTrue(captor.getValue().getBody().contains("trừ 3 điểm"));

        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendNotificationToVolunteersOfEvent_success() {
        UUID eventId = UUID.randomUUID();

        AnnounceVolunteerRequest req = new AnnounceVolunteerRequest();
        req.setTitle("title");
        req.setBody("body");

        service.sendNotificationToVolunteersOfEvent(eventId, req);

        verify(notificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), isNull());
    }

    @Test
    void sentEventCancelledByHostNotification_success() {
        UUID volId = UUID.randomUUID();

        Volunteer vol = new Volunteer();
        vol.setId(volId);

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setVolunteer(vol);
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        service.sentEventCancelledByHostNotification(List.of(app), "event", "reason");

        verify(notificationRepository).saveAll(any());
        verify(userNotificationRepository).saveAll(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendEventUpdateNonCriticalApprovedByOrgManagerNotification_list() {
        UUID volId = UUID.randomUUID();

        Volunteer vol = new Volunteer();
        vol.setId(volId);

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setVolunteer(vol);

        when(notificationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        service.sendEventUpdateNonCriticalApprovedByOrgManagerNotification(List.of(app), "event");

        verify(notificationRepository).saveAll(any());
        verify(userNotificationRepository).saveAll(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendEventUpdateCriticalApprovedByAdminNotification_success() {
        UUID hostId = UUID.randomUUID();
        UUID mngId = UUID.randomUUID();

        OrganizationManager mng = new OrganizationManager();
        mng.setId(mngId);

        Organization org = new Organization();
        org.setOrganizationManager(mng);

        Host host = new Host();
        host.setId(hostId);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");
        event.setHost(host);
        event.setOrganization(org);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(any())).thenReturn(new User());

        service.sendEventUpdateCriticalApprovedByAdminNotification(event);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository, times(2)).save(any());

        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
        verify(notificationPublisher).enqueueNotification(any(), eq(mngId));
    }

    @Test
    void sendEventUpdateCriticalApprovedByAdminNotification_list_success() {
        UUID volId = UUID.randomUUID();

        Volunteer vol = new Volunteer();
        vol.setId(volId);

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setSessionDate(LocalDate.now());
        app.setVolunteer(vol);

        when(notificationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendEventUpdateCriticalApprovedByAdminNotification(
                List.of(app), "event"
        );

        verify(notificationRepository).saveAll(any());
        verify(userNotificationRepository).saveAll(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendEventUpdateCriticalRejectedByAdminNotification_success() {
        UUID hostId = UUID.randomUUID();
        UUID mngId = UUID.randomUUID();

        OrganizationManager mng = new OrganizationManager();
        mng.setId(mngId);

        Organization org = new Organization();
        org.setOrganizationManager(mng);

        Host host = new Host();
        host.setId(hostId);

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");
        event.setHost(host);
        event.setOrganization(org);

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(any())).thenReturn(new User());

        service.sendEventUpdateCriticalRejectedByAdminNotification(event);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository, times(2)).save(any());

        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
        verify(notificationPublisher).enqueueNotification(any(), eq(mngId));
    }

    @Test
    void sendEventAssignedHostNotification_success() {
        UUID oldHostId = UUID.randomUUID();
        UUID newHostId = UUID.randomUUID();

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(any())).thenReturn(new User());

        service.sendEventAssignedHostNotification(oldHostId, newHostId, event);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository, times(2)).save(any());

        verify(notificationPublisher).enqueueNotification(any(), eq(oldHostId));
        verify(notificationPublisher).enqueueNotification(any(), eq(newHostId));
    }

    @Test
    void sendVolunteerReviewedByHostNotification_success() {
        UUID volId = UUID.randomUUID();

        Event event = new Event();
        event.setName("event");

        EventApplication app = new EventApplication();
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendVolunteerReviewedByHostNotification(
                volId, event, app, UUID.randomUUID()
        );

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendVolunteerReceivedCertificateNotification_success() {
        UUID volId = UUID.randomUUID();

        Volunteer vol = new Volunteer();
        vol.setId(volId);

        Event event = new Event();
        event.setName("event");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendVolunteerReceivedCertificateNotification(vol, event);

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendVolunteersReceivedCertificatesNotifications_success() {
        UUID v1 = UUID.randomUUID();
        UUID v2 = UUID.randomUUID();

        Volunteer vol1 = new Volunteer(); vol1.setId(v1);
        Volunteer vol2 = new Volunteer(); vol2.setId(v2);

        Event event = new Event();
        event.setName("event");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(any())).thenReturn(new User());

        service.sendVolunteersReceivedCertificatesNotifications(
                List.of(vol1, vol2), event
        );

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).saveAll(any());

        verify(notificationPublisher).enqueueNotification(any(), eq(v1));
        verify(notificationPublisher).enqueueNotification(any(), eq(v2));
    }

    @Test
    void sendEventCompletedNotifications_success() {
        UUID hostId = UUID.randomUUID();
        UUID mngId = UUID.randomUUID();

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("event");

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(any())).thenReturn(new User());

        service.sendEventCompletedNotifications(event, mngId, hostId);

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository, times(2)).save(any());

        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
        verify(notificationPublisher).enqueueNotification(any(), eq(mngId));
    }

    @Test
    void sendCheckInCodeOfEventSessionNotifications_success() {
        UUID volId = UUID.randomUUID();

        Volunteer vol = new Volunteer();
        vol.setId(volId);

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setVolunteer(vol);

        when(notificationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendCheckInCodeOfEventSessionNotifications(
                List.of(app), "event", "123456"
        );

        verify(notificationRepository).saveAll(any());
        verify(userNotificationRepository).saveAll(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendEventSessionHostedTodayNotification_success() {
        UUID hostId = UUID.randomUUID();

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(hostId)).thenReturn(new User());

        service.sendEventSessionHostedTodayNotification(hostId, UUID.randomUUID(), "event");

        verify(notificationRepository, times(2)).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(hostId));
    }

    @Test
    void sendClaimApprovedByHostNotification_success() {
        UUID volId = UUID.randomUUID();

        Event event = new Event();
        event.setName("event");

        EventApplication app = new EventApplication();
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendClaimApprovedByHostNotification(volId, event, app);

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

    @Test
    void sendClaimRejectedByHostNotification_success() {
        UUID volId = UUID.randomUUID();

        Event event = new Event();
        event.setName("event");

        EventApplication app = new EventApplication();
        app.setSessionDate(LocalDate.now());

        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.getReferenceById(volId)).thenReturn(new User());

        service.sendClaimRejectedByHostNotification(volId, event, app);

        verify(notificationRepository).save(any());
        verify(userNotificationRepository).save(any());
        verify(notificationPublisher).enqueueNotification(any(), eq(volId));
    }

}
