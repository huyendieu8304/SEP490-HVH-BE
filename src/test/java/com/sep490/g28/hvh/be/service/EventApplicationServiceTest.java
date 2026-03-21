package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
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
        EventApplication app = new EventApplication();
        app.setStatus(EEventApplicationStatus.PENDING);

        Volunteer volunteer = new Volunteer();
        volunteer.setId(UUID.randomUUID());
        volunteer.setEmail("test@mail.com");
        volunteer.setPhone("0123");
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setAvatarUrl(null);

        app.setVolunteer(volunteer);

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
                .getEventApplicationsByVolunteerIdAndSessionId(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(null);

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
                .getEventApplicationsByVolunteerIdAndSessionId(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(null);

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
                .getEventApplicationsByVolunteerIdAndSessionId(any(), any()))
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
                .getEventApplicationsByVolunteerIdAndSessionId(any(), any()))
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
                .getEventApplicationsByVolunteerIdAndSessionId(any(), any()))
                .thenReturn(Optional.empty());

        when(eventApplicationRepository.findOverlapSession(any(), any(), any(), any()))
                .thenReturn(new EventSession());

        assertThrows(AppException.class,
                () -> service.applyEventSession(s.getId()));
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
}
