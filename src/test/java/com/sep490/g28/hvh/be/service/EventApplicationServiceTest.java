package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckOutEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.EventApplicationServiceImpl;
import com.sep490.g28.hvh.be.util.GeoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EventApplicationServiceTest {
    @InjectMocks
    EventApplicationServiceImpl service;

    @Mock
    EventSessionRepository eventSessionRepository;
    @Mock
    EventApplicationRepository eventApplicationRepository;
    @Mock
    VolunteerRepository volunteerRepository;
    @Mock
    CurrentUserProvider currentUserProvider;
    @Mock
    StorageService storageService;
    @Mock
    CheckInLogRepository checkInLogRepository;
    @Mock
    EventRepository eventRepository;

    @Mock NotificationService notificationService;

    UUID volunteerId;
    UUID sessionId;
    UUID eventId;
    UUID hostId;

    @BeforeEach
    void setup() {
        volunteerId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        hostId = UUID.randomUUID();

        lenient().when(currentUserProvider.getId())
                .thenReturn(volunteerId);

        lenient().when(volunteerRepository.getReferenceById(any()))
                .thenReturn(new Volunteer());
    }

    private EventSession session(boolean autoApprove) {

        Event event = new Event();
        event.setStatus(EEventStatus.RECRUITING);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(5));
        event.setAutoApprove(autoApprove);

        EventSession s = new EventSession();
        s.setId(UUID.randomUUID());
        s.setEvent(event);
        s.setStartDateTime(OffsetDateTime.now().plusDays(20));
        s.setEndDateTime(OffsetDateTime.now().plusDays(20).plusHours(2));
        s.setExpectedVolAmount(10);
        s.setApprovedApplicationCount(0);

        return s;
    }

    private EventApplication application() {

        Event event = new Event();
        event.setStatus(EEventStatus.RECRUITING);

        EventSession session = new EventSession();
        session.setEvent(event);
        session.setExpectedVolAmount(10);
        session.setApprovedApplicationCount(0);

        Volunteer vol = new Volunteer();
        vol.setId(UUID.randomUUID());

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setStatus(EEventApplicationStatus.PENDING);
        app.setSession(session);
        app.setVolunteer(vol);

        return app;
    }

    private EventApplication application(EEventApplicationStatus status,
                                         EEventStatus eventStatus) {

        EventImage eventImages = new EventImage();
        eventImages.setImagePath("img1");

        Event event = new Event();
        event.setStatus(eventStatus);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(1));
        event.setImages(List.of(eventImages));

        EventSession session = new EventSession();
        session.setEvent(event);
        session.setApprovedApplicationCount(5);

        Volunteer vol = new Volunteer();
        vol.setId(UUID.randomUUID());
        vol.setHonorScore((short) 10);

        EventApplication app = new EventApplication();
        app.setId(UUID.randomUUID());
        app.setStatus(status);
        app.setSession(session);
        app.setVolunteer(vol);
        app.setSessionDate(LocalDate.now().plusDays(5));

        return app;
    }

    private Event mockEvent() {

        Event event = new Event();
        event.setId(eventId);
        event.setName("Charity Event");
        event.setDescription("Helping people");
        event.setAddress("Hanoi");
        event.setDetailAddress("Hanoi");
        event.setServedTarget(EServedTarget.CHILDREN);
        event.setServingPlaceType(EServingPlaceType.CEMETERY);
        event.setStartDate(LocalDate.now());
        event.setRecruitmentEndDate(LocalDate.now().minusDays(1));
        event.setStatus(EEventStatus.RECRUITING);

        Organization org = new Organization();
        org.setName("Volunteer Org");
        event.setOrganization(org);

        Host host = new Host();
        host.setId(hostId);
        host.setFullName("Host A");
        host.setPhone("0901234567");
        event.setHost(host);

        ActivitySubDomain subDomain = new ActivitySubDomain();
        subDomain.setName("Education");
        event.setActivitySubDomain(subDomain);

        EventImage img1 = new EventImage();
        img1.setImagePath("img1");

        EventImage img2 = new EventImage();
        img2.setImagePath("img2");

        event.setImages(List.of(img1, img2));

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setExpectedVolAmount(5);
        session.setExpectedSerAmount(10);
        session.setApprovedApplicationCount(0);

        event.setSessions(List.of(session));

        return event;
    }

    private CheckEventCheckInCodeRequest validCheckEventCheckInCodeRequest() {
        CheckEventCheckInCodeRequest req = new CheckEventCheckInCodeRequest();
        req.setCheckInCode("123456");
        return req;
    }

    private QuickCheckInEventRequest validQuickCheckInEventRequest() {
        QuickCheckInEventRequest request = new QuickCheckInEventRequest();
        request.setEventSessionId(sessionId.toString());
        request.setDeviceId("device-1");
        request.setApVersion("1.0");
        request.setOsVersion("android");
        request.setCurrentPlaceLat(10.0);
        request.setCurrentPlaceLng(10.0);
        return request;
    }

    private CheckOutEventRequest validCheckOutEventRequest() {
        CheckOutEventRequest request = new CheckOutEventRequest();
        request.setEventSessionId(sessionId.toString());
        request.setDeviceId("device-1");
        request.setApVersion("1.0");
        request.setOsVersion("android");
        request.setCurrentPlaceLat(10.0);
        request.setCurrentPlaceLng(10.0);
        return request;
    }

    //----- applyEventSession --------------------------
    // TC01
    @Test
    void applyEventSession_success_pending() {

        EventSession s = session(false);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        when(eventApplicationRepository
                .findApplicationPendingOrApproved(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(null);

        when(eventApplicationRepository.save(any()))
                .thenAnswer(invocation -> {
                    EventApplication app = invocation.getArgument(0);
                    app.setId(UUID.randomUUID());
                    return app;
                });

        service.applyEventSession(s.getId());

        verify(eventApplicationRepository).save(argThat(app ->
                app.getStatus() == EEventApplicationStatus.PENDING
        ));
    }

    //TC02
    @Test
    void applyEventSession_autoApprove_shouldApproved() {

        EventSession s = session(true);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        when(eventApplicationRepository
                .findApplicationPendingOrApproved(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(null);

        when(eventApplicationRepository.save(any()))
                .thenAnswer(invocation -> {
                    EventApplication app = invocation.getArgument(0);
                    app.setId(UUID.randomUUID());
                    return app;
                });

        service.applyEventSession(s.getId());

        assertEquals(1, s.getApprovedApplicationCount());

        verify(eventSessionRepository).save(s);

        verify(eventApplicationRepository).save(argThat(app ->
                app.getStatus() == EEventApplicationStatus.APPROVED
        ));
    }

//    TC03
    @Test
    void applyEventSession_notExist_shouldThrow() {

        when(eventSessionRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.applyEventSession(UUID.randomUUID()));
    }

    //TC04
    @Test
    void applyEventSession_eventNotRecruiting_shouldThrow() {

        EventSession s = session(false);
        s.getEvent().setStatus(EEventStatus.APPROVED_BY_MNG);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
    }

    //TC05
    @Test
    void applyEventSession_recruitmentClosed_shouldThrow() {

        EventSession s = session(false);
        s.getEvent().setRecruitmentEndDate(LocalDate.now().minusDays(1));

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
    }

    //TC06
    @Test
    void applyEventSession_alreadyApplied_shouldThrow() {

        EventSession s = session(false);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        when(eventApplicationRepository
                .findApplicationPendingOrApproved(any(), any()))
                .thenReturn(Optional.of(new EventApplication()));

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
    }

    //TC07
    @Test
    void applyEventSession_sessionFull_shouldThrow() {

        EventSession s = session(false);
        s.setApprovedApplicationCount(10);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        when(eventApplicationRepository
                .findApplicationPendingOrApproved(any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
    }

    //TC08
    @Test
    void applyEventSession_overlap_shouldThrow() {

        EventSession s = session(false);

        when(eventSessionRepository.findById(s.getId()))
                .thenReturn(Optional.of(s));

        when(eventApplicationRepository
                .findApplicationPendingOrApproved(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(new EventSession());

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
    }

    //----- approveApplication ------------------------------------------
    // TC01
    @Test
    void approveApplication_success() {

        EventApplication app = application();

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.approveApplication(app.getId());

        assertEquals(EEventApplicationStatus.APPROVED, app.getStatus());

        assertEquals(1,
                app.getSession().getApprovedApplicationCount());

        verify(eventApplicationRepository).save(app);
        verify(eventSessionRepository).save(app.getSession());

        verify(notificationService).sendEventApplicationApprovedNotification(
                eq(app.getVolunteer().getId()),
                eq(app.getSession().getEvent()),
                eq(app)
        );
    }

    // TC02
    @Test
    void approveApplication_notExist_shouldThrow() {

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.approveApplication(UUID.randomUUID()));
    }

    // TC03
    @Test
    void approveApplication_notPending_shouldThrow() {

        EventApplication app = application();
        app.setStatus(EEventApplicationStatus.APPROVED);

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.approveApplication(app.getId()));
    }

    // TC04
    @Test
    void approveApplication_sessionFull_shouldThrow() {

        EventApplication app = application();
        app.getSession().setApprovedApplicationCount(10);

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.approveApplication(app.getId()));
    }

    // TC05
    @Test
    void approveApplication_eventNotRecruiting_shouldThrow() {

        EventApplication app = application();
        app.getSession().getEvent().setStatus(EEventStatus.APPROVED_BY_MNG);

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.approveApplication(app.getId()));
    }

    // ----- rejectApplication---------------------------
    // TC01
    @Test
    void rejectApplication_success() {

        EventApplication app = application();

        RejectApplicationRequest req = new RejectApplicationRequest();
        req.setRejectionReason("invalid");

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.rejectApplication(app.getId(), req);

        assertEquals(EEventApplicationStatus.REJECTED, app.getStatus());

        verify(eventApplicationRepository).save(app);

        verify(notificationService).sendEventApplicationRejectedNotification(
                eq(app.getVolunteer().getId()),
                eq(app.getSession().getEvent()),
                eq(app),
                eq(req.getRejectionReason())
        );
    }

    // TC02
    @Test
    void rejectApplication_notExist_shouldThrow() {

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.empty());

        RejectApplicationRequest req = new RejectApplicationRequest();
        req.setRejectionReason("reason");

        assertThrows(AppException.class,
                () -> service.rejectApplication(UUID.randomUUID(), req));
    }

    // TC03
    @Test
    void rejectApplication_notPending_shouldThrow() {

        EventApplication app = application();
        app.setStatus(EEventApplicationStatus.APPROVED);

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        RejectApplicationRequest req = new RejectApplicationRequest();
        req.setRejectionReason("reason");

        assertThrows(AppException.class,
                () -> service.rejectApplication(app.getId(), req));
    }

    //------ cancelApplication -----------------------
    // TC01
    @Test
    void cancelApplication_pending_success() {

        EventApplication app = application(
                EEventApplicationStatus.PENDING,
                EEventStatus.RECRUITING
        );

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.cancelApplication(app.getId());

        assertEquals(EEventApplicationStatus.CANCELLED, app.getStatus());

        verify(eventApplicationRepository).save(app);

        verify(notificationService).sendEventApplicationCancelledSuccessfullyNotification(
                eq(app.getVolunteer().getId()),
                eq(app.getSession().getEvent()),
                eq(app),
                eq(false)
        );

        verifyNoInteractions(volunteerRepository);
        verifyNoInteractions(eventSessionRepository);
    }

    // TC02
    @Test
    void cancelApplication_approved_minusScore() {

        EventApplication app = application(
                EEventApplicationStatus.APPROVED,
                EEventStatus.RECRUITING
        );

        // today > recruitmentEndDate && < sessionDate
        app.getSession().getEvent().setRecruitmentEndDate(LocalDate.now().minusDays(2));
        app.setSessionDate(LocalDate.now().plusDays(2));

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.cancelApplication(app.getId());

        assertEquals((short) 7, app.getVolunteer().getHonorScore());

        verify(volunteerRepository).save(app.getVolunteer());
        verify(eventSessionRepository).save(app.getSession());

        assertEquals(4, app.getSession().getApprovedApplicationCount());

        verify(notificationService).sendEventApplicationCancelledSuccessfullyNotification(
                eq(app.getVolunteer().getId()),
                any(),
                eq(app),
                eq(true)
        );
    }

    // TC03
    @Test
    void cancelApplication_approved_noMinusScore() {

        EventApplication app = application(
                EEventApplicationStatus.APPROVED,
                EEventStatus.RECRUITING
        );

        // today <= recruitmentEndDate
        app.getSession().getEvent().setRecruitmentEndDate(LocalDate.now().plusDays(1));

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.cancelApplication(app.getId());

        assertEquals((short) 10, app.getVolunteer().getHonorScore());

        verify(eventSessionRepository).save(app.getSession());

        verify(notificationService).sendEventApplicationCancelledSuccessfullyNotification(
                eq(app.getVolunteer().getId()),
                any(),
                eq(app),
                eq(false)
        );
    }

    // TC04
    @Test
    void cancelApplication_notExist_shouldThrow() {

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.cancelApplication(UUID.randomUUID()));
    }

    // TC05
    @Test
    void cancelApplication_eventNotAllowed_shouldThrow() {

        EventApplication app = application(
                EEventApplicationStatus.PENDING,
                EEventStatus.CANCELLED // giả sử không cho cancel
        );

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.cancelApplication(app.getId()));
    }

    // TC06
    @Test
    void cancelApplication_alreadyCancelled_shouldThrow() {

        EventApplication app = application(
                EEventApplicationStatus.CANCELLED,
                EEventStatus.RECRUITING
        );

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.cancelApplication(app.getId()));
    }

    // TC07
    @Test
    void cancelApplication_rejected_shouldThrow() {

        EventApplication app = application(
                EEventApplicationStatus.REJECTED,
                EEventStatus.RECRUITING
        );

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.cancelApplication(app.getId()));
    }

    // TC08
    @Test
    void cancelApplication_minusScore_exactCondition() {

        EventApplication app = application(
                EEventApplicationStatus.APPROVED,
                EEventStatus.RECRUITING
        );

        LocalDate today = LocalDate.now();

        app.getSession().getEvent()
                .setRecruitmentEndDate(today.minusDays(1)); // today > recruitmentEndDate

        app.setSessionDate(today.plusDays(1)); // today < sessionDate

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.cancelApplication(app.getId());

        assertEquals((short) 7, app.getVolunteer().getHonorScore());

        verify(volunteerRepository).save(app.getVolunteer());

        verify(notificationService).sendEventApplicationCancelledSuccessfullyNotification(
                eq(app.getVolunteer().getId()),
                any(),
                eq(app),
                eq(true)
        );
    }

    // TC09
    @Test
    void cancelApplication_todayEqualsSessionDate_shouldThrow() {

        EventApplication app = application(
                EEventApplicationStatus.APPROVED,
                EEventStatus.ONGOING
        );

        LocalDate today = LocalDate.now();

        app.getSession().getEvent()
                .setRecruitmentEndDate(today.minusDays(2));

        app.setSessionDate(today); // today == sessionDate

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> service.cancelApplication(app.getId()));
    }

    // TC10
    @Test
    void cancelApplication_todayEqualsRecruitmentEndDate_noMinus() {

        EventApplication app = application(
                EEventApplicationStatus.APPROVED,
                EEventStatus.RECRUITING
        );

        LocalDate today = LocalDate.now();

        app.getSession().getEvent()
                .setRecruitmentEndDate(today); // today == recruitmentEndDate

        app.setSessionDate(today.plusDays(2));

        when(eventApplicationRepository.findById(app.getId()))
                .thenReturn(Optional.of(app));

        service.cancelApplication(app.getId());

        assertEquals((short)10, app.getVolunteer().getHonorScore());

        verify(volunteerRepository, never()).save(any());
    }

    //----- getRegisteredParticipants --------------------------
    //TC01
    @Test
    void getRegisteredParticipants_success() {

        EventApplication app = application();
        app.getVolunteer().setAvatarUrl("avatar1");

        Page<EventApplication> page = new PageImpl<>(List.of(app));

        when(eventApplicationRepository.getEventApplicationsBySessionId(
                eq(sessionId),
                any()
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("avatar1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        EventApplicationsResponse response =
                service.getRegisteredParticipants(0, 10, sessionId);

        assertEquals(1, response.getRegisteredParticipants().size());
        assertEquals("signed-url",
                response.getRegisteredParticipants().getFirst().getAvatarUrl());
    }

    //TC02
    @Test
    void getRegisteredParticipants_volunteer_null() {

        EventApplication app = application();
        app.setVolunteer(null);

        Page<EventApplication> page = new PageImpl<>(List.of(app));

        when(eventApplicationRepository.getEventApplicationsBySessionId(
                eq(sessionId),
                any()
        )).thenReturn(page);

        EventApplicationsResponse response =
                service.getRegisteredParticipants(0, 10, sessionId);

        RegisteredParticipantSimpleResponse participant =
                response.getRegisteredParticipants().getFirst();

        assertNull(participant.getVolunteerId());
        assertNull(participant.getEmail());
        assertNull(participant.getAvatarUrl());
    }

    //TC03
    @Test
    void getRegisteredParticipants_should_return_null_next_page() {

        EventApplication app = application();

        Page<EventApplication> page = new PageImpl<>(
                List.of(app),
                PageRequest.of(0, 10),
                1
        );

        when(eventApplicationRepository.getEventApplicationsBySessionId(
                eq(sessionId),
                any()
        )).thenReturn(page);

        EventApplicationsResponse response =
                service.getRegisteredParticipants(0, 10, sessionId);

        assertFalse(response.isHasMore());
        assertNull(response.getNextCursor());
    }

    //TC04
    @Test
    void getRegisteredParticipants_should_return_empty_page() {

        Page<EventApplication> page = new PageImpl<>(new ArrayList<>());

        when(eventApplicationRepository.getEventApplicationsBySessionId(
                eq(sessionId),
                any()
        )).thenReturn(page);

        EventApplicationsResponse response =
                service.getRegisteredParticipants(0, 10, sessionId);

        assertEquals(new ArrayList<>(), response.getRegisteredParticipants());
    }

    // ==== getEventApplicationsStatus ===================================
    // ===== TC1 =====
    @Test
    void getEventApplicationsStatus_success() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);



        EventApplication eventApplication = application();

        EventImage eventImage = new EventImage();
        eventImage.setImagePath("img1");
        eventImage.setEvent(eventApplication.getSession().getEvent());

        eventApplication.getSession().getEvent().setImages(List.of(eventImage));

        Page<EventApplication> page = new PageImpl<>(List.of(eventApplication));

        when(eventApplicationRepository.findByVolunteerId(
                eq(volunteerId),
                any(),
                any()
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        Page<EventApplicationsStatusResponse> response =
                service.getEventApplicationsStatus(0, 10, "PENDING");

        assertEquals(1, response.getContent().size());
        assertEquals("signed-url",
                response.getContent().getFirst().getImageUrl());
    }

    // ===== TC2 =====
    @Test
    void getEventApplicationsStatus_no_application() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<EventApplication> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(eventApplicationRepository.findByVolunteerId(
                eq(volunteerId),
                any(),
                any()
        )).thenReturn(emptyPage);

        Page<EventApplicationsStatusResponse> response =
                service.getEventApplicationsStatus(0, 10, "PENDING");

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    // ===== TC3 =====
    @Test
    void getEventApplicationsStatus_parse_null_and_without_image() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication eventApplication = application();
        eventApplication.getSession().getEvent().setImages(null);

        Page<EventApplication> page = new PageImpl<>(List.of(eventApplication));

        when(eventApplicationRepository.findByVolunteerId(
                eq(volunteerId),
                any(),
                any()
        )).thenReturn(page);

        Page<EventApplicationsStatusResponse> response =
                service.getEventApplicationsStatus(0, 10,  null);

        assertEquals(1, response.getContent().size());
        assertNull(response.getContent().getFirst().getImageUrl());
    }

    // ==== checkEventCheckInCode ===================================
    // ===== TC1 =====
    @Test
    void checkEventCheckInCode_success() {

        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setStatus(EEventStatus.ONGOING);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(2));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setEvent(event);
        session.setCheckInCode("123456");

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        CheckEventCheckInCodeResponse response =
                service.checkEventCheckInCode(request);

        assertEquals(eventId, response.getEventId());
        assertEquals(sessionId, response.getEventSessionId());
    }

    // ===== TC2 =====
    @Test
    void checkEventCheckInCode_application_not_exist() {

        UUID volunteerId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(null);

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC3 =====
    @Test
    void checkEventCheckInCode_session_not_exist() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        EventSession session = new EventSession();
        session.setId(sessionId);
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC4 =====
    @Test
    void checkEventCheckInCode_session_not_started() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().plusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2)); // future

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC5 =====
    @Test
    void checkEventCheckInCode_session_ended() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(2));
        session.setEndDateTime(OffsetDateTime.now().minusHours(1)); // future

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC6 =====
    @Test
    void checkEventCheckInCode_event_not_exist() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC7 =====
    @Test
    void checkEventCheckInCode_event_not_ongoing() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);
        event.setStatus(EEventStatus.UPCOMING); // not ONGOING

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().plusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC8 =====
    @Test
    void checkEventCheckInCode_code_not_match() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);
        event.setStatus(EEventStatus.ONGOING);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));
        session.setEvent(event);
        session.setCheckInCode("234567");

        EventApplication app = new EventApplication();
        app.setSession(session);

        when(eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(eq(volunteerId), any()))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ===== TC9 =====
    @Test
    void checkEventCheckInCode_already_checked_in() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        CheckEventCheckInCodeRequest request = validCheckEventCheckInCodeRequest();

        assertThrows(
                AppException.class,
                () -> service.checkEventCheckInCode(request)
        );
    }

    // ==== quickCheckInEvent ===================================
    // ===== TC1 =====
    @Test
    void quickCheckInEvent_success() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setCheckInAccuracyMeters(100.0);
        event.setStatus(EEventStatus.ONGOING);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setId(sessionId);
        session.setEvent(event);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(checkInLogRepository
                .existsByDevice(any(), any(), any()))
                .thenReturn(false);

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volunteerId);
        volunteer.setDeviceId("device-1");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        try (MockedStatic<GeoUtils> geoMock = mockStatic(GeoUtils.class)) {
            Point mockPoint = mock(Point.class);

            geoMock.when(() -> GeoUtils.toPoint(any(Double.class), any(Double.class)))
                    .thenReturn(mockPoint);

            geoMock.when(() -> GeoUtils.distanceMeters(any(Point.class), any(Point.class)))
                    .thenReturn(50.0);

            service.quickCheckInEvent(request);
        }

        verify(checkInLogRepository).save(any(CheckInLog.class));
    }

    // ===== TC2 =====
    @Test
    void quickCheckInEvent_already_checked_in() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }

    // ===== TC3 =====
    @Test
    void quickCheckInEvent_session_not_exist() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }

    // ===== TC4 =====
    @Test
    void quickCheckInEvent_out_of_range() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setCheckInAccuracyMeters(100.0);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setId(sessionId);
        session.setEvent(event);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();
        request.setCurrentPlaceLat(10.0);
        request.setCurrentPlaceLng(10.0);

        try (MockedStatic<GeoUtils> geoMock = mockStatic(GeoUtils.class)) {
            geoMock.when(() -> GeoUtils.toPoint(anyDouble(), anyDouble()))
                    .thenReturn(mock(Point.class));
            geoMock.when(() -> GeoUtils.distanceMeters(any(), any()))
                    .thenReturn(200.0); // > accuracy

            assertThrows(
                    AppException.class,
                    () -> service.quickCheckInEvent(request)
            );
        }
    }

    // ===== TC5 =====
    @Test
    void quickCheckInEvent_device_already_used() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setCheckInAccuracyMeters(100.0);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setId(sessionId);
        session.setEvent(event);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        try (MockedStatic<GeoUtils> geoMock = mockStatic(GeoUtils.class)) {
            geoMock.when(() -> GeoUtils.toPoint(anyDouble(), anyDouble()))
                    .thenReturn(mock(Point.class));
            geoMock.when(() -> GeoUtils.distanceMeters(any(), any()))
                    .thenReturn(50.0);

            assertThrows(
                    AppException.class,
                    () -> service.quickCheckInEvent(request)
            );
        }
    }

    // ===== TC6 =====
    @Test
    void quickCheckInEvent_volunteer_not_exist() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setCheckInAccuracyMeters(100.0);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setId(sessionId);
        session.setEvent(event);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        try (MockedStatic<GeoUtils> geoMock = mockStatic(GeoUtils.class)) {
            geoMock.when(() -> GeoUtils.toPoint(anyDouble(), anyDouble()))
                    .thenReturn(mock(Point.class));
            geoMock.when(() -> GeoUtils.distanceMeters(any(), any()))
                    .thenReturn(50.0);

            assertThrows(
                    AppException.class,
                    () -> service.quickCheckInEvent(request)
            );
        }
    }

    // ===== TC7 =====
    @Test
    void quickCheckInEvent_device_not_match() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = mockEvent();
        event.setCheckInAccuracyMeters(100.0);
        event.setStatus(EEventStatus.ONGOING);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setId(sessionId);
        session.setEvent(event);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volunteerId);
        volunteer.setDeviceId("device-1");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();
        request.setDeviceId("device-2");

        try (MockedStatic<GeoUtils> geoMock = mockStatic(GeoUtils.class)) {
            geoMock.when(() -> GeoUtils.toPoint(anyDouble(), anyDouble()))
                    .thenReturn(mock(Point.class));
            geoMock.when(() -> GeoUtils.distanceMeters(any(), any()))
                    .thenReturn(50.0);

            service.quickCheckInEvent(request);
        }

        verify(checkInLogRepository, never()).save(any());
    }

    // ===== TC8 =====
    @Test
    void quickCheckInEvent_session_not_started() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().plusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2)); // future

        EventApplication app = new EventApplication();
        app.setSession(session);

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }

    // ===== TC9 =====
    @Test
    void quickCheckInEvent_session_ended() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(2));
        session.setEndDateTime(OffsetDateTime.now().minusHours(1)); // future

        EventApplication app = new EventApplication();
        app.setSession(session);

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }

    // ===== TC10 =====
    @Test
    void quickCheckInEvent_event_not_exist() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setSession(session);

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }

    // ===== TC11 =====
    @Test
    void quickCheckInEvent_event_not_ongoing() {

        UUID volunteerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);
        event.setStatus(EEventStatus.UPCOMING); // not ONGOING

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().plusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setSession(session);

        QuickCheckInEventRequest request = validQuickCheckInEventRequest();

        assertThrows(
                AppException.class,
                () -> service.quickCheckInEvent(request)
        );
    }
}
