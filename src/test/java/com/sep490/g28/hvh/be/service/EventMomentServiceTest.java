package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import com.sep490.g28.hvh.be.dto.eventmoment.response.ShareMomentResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventMoment;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.EventMomentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventMomentServiceTest {

    @Mock
    EventSessionRepository eventSessionRepository;

    @Mock
    EventApplicationRepository eventApplicationRepository;

    @Mock
    EventMomentRepository eventMomentRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    StorageService storageService;

    @Mock
    StoragePathGenerator storagePathGenerator;

    @InjectMocks
    EventMomentServiceImpl service;

    UUID volunteerId;
    UUID eventId;
    UUID hostId;
    UUID sessionId;

    @BeforeEach
    void setup() {

        volunteerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        hostId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    private ShareMomentRequest validShareMomentRequest() {
        ShareMomentRequest request = new ShareMomentRequest();
        request.setEventSessionId(sessionId.toString());
        request.setMomentPictures("img1 img2 img3");
        request.setMomentContent("content");
        return request;
    }

    // ==== shareMoment ===================================
    // ===== TC1 =====
    @Test
    void shareMoment_success() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));
        session.setEvent(event);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventMomentRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        when(storagePathGenerator.eventMomentImages(any(), any(), anyInt(), any()))
                .thenAnswer(inv -> "path-" + inv.getArgument(1));

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        ShareMomentRequest request = validShareMomentRequest();
        request.setMomentPictures("img1 img2 img3");

        ShareMomentResponse response = service.shareMoment(request);

        assertNotNull(response);
        assertEquals(3, response.getMomentPicturesUploadUrls().size());

        verify(eventMomentRepository).save(any(EventMoment.class));
    }

    // ===== TC2 =====
    @Test
    void shareMoment_application_not_exist() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(null);

        ShareMomentRequest request = validShareMomentRequest();

        assertThrows(AppException.class,
                () -> service.shareMoment(request));
    }

    // ===== TC3 =====
    @Test
    void shareMoment_session_not_exist() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ShareMomentRequest request = validShareMomentRequest();

        assertThrows(AppException.class,
                () -> service.shareMoment(request));
    }

    // ===== TC4 =====
    @Test
    void shareMoment_session_not_started() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now().plusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(2));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ShareMomentRequest request = validShareMomentRequest();

        assertThrows(AppException.class,
                () -> service.shareMoment(request));
    }

    // ===== TC5 =====
    @Test
    void shareMoment_session_ended() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now().minusHours(2));
        session.setEndDateTime(OffsetDateTime.now().minusHours(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ShareMomentRequest request = validShareMomentRequest();

        assertThrows(AppException.class,
                () -> service.shareMoment(request));
    }

    // ===== TC6 =====
    @Test
    void shareMoment_already_shared() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventMomentRepository.findByEventApplicationId(applicationId))
                .thenReturn(new EventMoment());

        ShareMomentRequest request = validShareMomentRequest();

        assertThrows(AppException.class,
                () -> service.shareMoment(request));
    }

    // ===== TC7 =====
    @Test
    void shareMoment_limit_5_images() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(eventId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));
        session.setEvent(event);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventMomentRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        when(storagePathGenerator.eventMomentImages(any(), any(), anyInt(), any()))
                .thenAnswer(inv -> "path-" + inv.getArgument(1));

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        ShareMomentRequest request = validShareMomentRequest();
        request.setMomentPictures("a b c d e f"); // >4

        ShareMomentResponse response = service.shareMoment(request);

        assertEquals(5, response.getMomentPicturesUploadUrls().size());
    }

    // ===== TC8 =====
    @Test
    void shareMoment_no_images() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now().minusHours(1));
        session.setEndDateTime(OffsetDateTime.now().plusHours(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventMomentRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        ShareMomentRequest request = validShareMomentRequest();
        request.setMomentPictures(null);

        ShareMomentResponse response = service.shareMoment(request);

        assertNotNull(response);
        assertEquals(0, response.getMomentPicturesUploadUrls().size());
    }
}
