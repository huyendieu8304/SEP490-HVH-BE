package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.impl.EventApplicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    @Mock NotificationService notificationService;

    UUID volunteerId;
    UUID sessionId;

    @BeforeEach
    void setup() {
        volunteerId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

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

        Event event = new Event();
        event.setStatus(eventStatus);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(1)); // default

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

        verify(notificationService).sendEventApplicationApproved(
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

        verify(notificationService).sendEventApplicationRejected(
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

        verify(notificationService).sendEventApplicationCancelledSuccessfully(
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

        verify(notificationService).sendEventApplicationCancelledSuccessfully(
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

        verify(notificationService).sendEventApplicationCancelledSuccessfully(
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

        verify(notificationService).sendEventApplicationCancelledSuccessfully(
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
}
