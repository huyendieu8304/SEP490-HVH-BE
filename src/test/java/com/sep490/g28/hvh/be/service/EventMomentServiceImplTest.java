package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedDetailsResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.ShareMomentResponse;
import com.sep490.g28.hvh.be.entity.*;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventMomentServiceImplTest {

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

    // ==== deleteEventMoment ===================================
    // ===== TC1 =====
    @Test
    void deleteEventMoment_success() {

        UUID momentId = UUID.randomUUID();

        EventMoment moment = new EventMoment();
        moment.setId(momentId);

        when(eventMomentRepository.findById(momentId))
                .thenReturn(Optional.of(moment));

        service.deleteEventMoment(momentId);

        verify(eventMomentRepository).delete(moment);
    }

    // ===== TC2 =====
    @Test
    void deleteEventMoment_not_found() {

        UUID momentId = UUID.randomUUID();

        when(eventMomentRepository.findById(momentId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.deleteEventMoment(momentId));
    }

    // ==== getEventMomentsForVolunteer ===================================
    // ===== TC1 =====
    @Test
    void getEventMomentsForVolunteer_success_full_data() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setAvatarUrl("avatar-path");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setName("Event A");
        event.setAddress("addr");
        event.setDetailAddress("detail");

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setId(UUID.randomUUID());
        moment.setEventApplication(app);
        moment.setMomentContent("content");
        moment.setMomentPictures("pic1 pic2");
        moment.setCreatedAt(OffsetDateTime.now());

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByVolunteerId(eq(volunteerId), any(), any()))
                .thenReturn(page);

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        when(storageService.getSignedUrlAsync("pic1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("pic2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsForVolunteer(0, 10, null);

        assertEquals(1, result.getContent().size());

        EventMomentFeedDetailsResponse res = result.getContent().getFirst();

        assertEquals("nick", res.getVolNickName());
        assertEquals("name", res.getVolName());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals(2, res.getMomentPicturesUrls().size());
    }

    // ===== TC2 =====
    @Test
    void getEventMomentsForVolunteer_volunteer_null() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Event event = new Event();
        event.setId(UUID.randomUUID());

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(null);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);
        moment.setMomentPictures(null);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByVolunteerId(eq(volunteerId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsForVolunteer(0, 10, null);

        EventMomentFeedDetailsResponse res = result.getContent().getFirst();

        assertNull(res.getVolNickName());
        assertNull(res.getAvatarUrl());
    }

    // ===== TC3 =====
    @Test
    void getEventMomentsForVolunteer_empty() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(eventMomentRepository.findAllByVolunteerId(eq(volunteerId), any(), any()))
                .thenReturn(Page.empty());

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsForVolunteer(0, 10, null);

        assertTrue(result.isEmpty());
    }

    // ===== TC4 =====
    @Test
    void getEventMomentsForVolunteer_avatar_null() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setAvatarUrl(null);

        EventSession session = new EventSession();
        session.setEvent(new Event());

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByVolunteerId(eq(volunteerId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsForVolunteer(0, 10, null);

        assertNull(result.getContent().getFirst().getAvatarUrl());
    }

    // ===== TC5 =====
    @Test
    void getEventMomentsForVolunteer_no_pictures() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventSession session = new EventSession();
        session.setEvent(new Event());

        EventApplication app = new EventApplication();
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);
        moment.setMomentPictures(null);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByVolunteerId(eq(volunteerId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsForVolunteer(0, 10, null);

        assertTrue(result.getContent().getFirst().getMomentPicturesUrls().isEmpty());
    }

    // ==== getEventMomentsOfEvent ===================================
    // ===== TC1 =====
    @Test
    void getEventMomentsOfEvent_success_full_data() {

        UUID eventId = UUID.randomUUID();
        UUID volId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volId);
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setAvatarUrl("avatar-path");

        Event event = new Event();
        event.setId(eventId);
        event.setName("Event A");
        event.setAddress("addr");
        event.setDetailAddress("detail");

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setId(UUID.randomUUID());
        moment.setEventApplication(app);
        moment.setMomentContent("content");
        moment.setMomentPictures("pic1 pic2");
        moment.setCreatedAt(OffsetDateTime.now());

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByEventId(eq(eventId), any(), any()))
                .thenReturn(page);

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        when(storageService.getSignedUrlAsync("pic1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("pic2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsOfEvent(0, 10, eventId, null);

        assertEquals(1, result.getContent().size());

        EventMomentFeedDetailsResponse res = result.getContent().getFirst();

        assertEquals(volId, res.getVolunteerId());
        assertEquals("nick", res.getVolNickName());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals(2, res.getMomentPicturesUrls().size());
    }

    // ===== TC2 =====
    @Test
    void getEventMomentsOfEvent_volunteer_null() {

        UUID eventId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(null);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByEventId(eq(eventId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsOfEvent(0, 10, eventId, null);

        EventMomentFeedDetailsResponse res = result.getContent().getFirst();

        assertNull(res.getVolunteerId());
        assertNull(res.getAvatarUrl());
    }

    // ===== TC3 =====
    @Test
    void getEventMomentsOfEvent_avatar_null() {

        UUID eventId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setId(UUID.randomUUID());
        volunteer.setAvatarUrl(null);

        EventSession session = new EventSession();
        session.setEvent(new Event());

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByEventId(eq(eventId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsOfEvent(0, 10, eventId, null);

        assertNull(result.getContent().getFirst().getAvatarUrl());
    }

    // ===== TC4 =====
    @Test
    void getEventMomentsOfEvent_no_pictures() {

        UUID eventId = UUID.randomUUID();

        EventSession session = new EventSession();
        session.setEvent(new Event());

        EventApplication app = new EventApplication();
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);
        moment.setMomentPictures(null);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByEventId(eq(eventId), any(), any()))
                .thenReturn(page);

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsOfEvent(0, 10, eventId, null);

        assertTrue(result.getContent().getFirst().getMomentPicturesUrls().isEmpty());
    }

    // ===== TC5 =====
    @Test
    void getEventMomentsOfEvent_empty() {

        UUID eventId = UUID.randomUUID();

        when(eventMomentRepository.findAllByEventId(eq(eventId), any(), any()))
                .thenReturn(Page.empty());

        Page<EventMomentFeedDetailsResponse> result =
                service.getEventMomentsOfEvent(0, 10, eventId, null);

        assertTrue(result.isEmpty());
    }

    // ==== getEventMomentsFeed ===================================
    // ===== TC1 =====
    @Test
    void getEventMomentsFeed_success_hasNext_true() {

        UUID eventId = UUID.randomUUID();
        UUID volId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volId);
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setAvatarUrl("avatar-path");

        Event event = new Event();
        event.setId(eventId);
        event.setName("Event A");
        event.setAddress("addr");
        event.setDetailAddress("detail");

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setId(UUID.randomUUID());
        moment.setEventApplication(app);
        moment.setMomentContent("content");
        moment.setMomentPictures("pic1 pic2");
        moment.setCreatedAt(OffsetDateTime.now());

        Page<EventMoment> page = new PageImpl<>(
                List.of(moment),
                PageRequest.of(0, 10),
                20 // total > size → hasNext = true
        );

        when(eventMomentRepository.findAllByName(any(), any()))
                .thenReturn(page);

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        when(storageService.getSignedUrlAsync("pic1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("pic2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        EventMomentFeedResponse response =
                service.getEventMomentsFeed(0, 10, null);

        assertEquals(1, response.getEventMoments().size());
        assertEquals("1", response.getNextCursor());
        assertTrue(response.isHasMore());
    }

    // ===== TC2 =====
    @Test
    void getEventMomentsFeed_hasNext_false() {

        Page<EventMoment> page = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10),
                0
        );

        when(eventMomentRepository.findAllByName(any(), any()))
                .thenReturn(page);

        EventMomentFeedResponse response =
                service.getEventMomentsFeed(0, 10, null);

        assertNull(response.getNextCursor());
        assertFalse(response.isHasMore());
    }

    // ===== TC3 =====
    @Test
    void getEventMomentsFeed_volunteer_null() {

        Event event = new Event();

        EventSession session = new EventSession();
        session.setEvent(event);

        EventApplication app = new EventApplication();
        app.setVolunteer(null);
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByName(any(), any()))
                .thenReturn(page);

        EventMomentFeedResponse response =
                service.getEventMomentsFeed(0, 10, null);

        EventMomentFeedDetailsResponse res = response.getEventMoments().getFirst();

        assertNull(res.getVolunteerId());
        assertNull(res.getAvatarUrl());
    }

    // ===== TC4 =====
    @Test
    void getEventMomentsFeed_no_pictures() {

        EventSession session = new EventSession();
        session.setEvent(new Event());

        EventApplication app = new EventApplication();
        app.setSession(session);

        EventMoment moment = new EventMoment();
        moment.setEventApplication(app);
        moment.setMomentPictures(null);

        Page<EventMoment> page = new PageImpl<>(List.of(moment));

        when(eventMomentRepository.findAllByName(any(), any()))
                .thenReturn(page);

        EventMomentFeedResponse response =
                service.getEventMomentsFeed(0, 10, null);

        assertTrue(response.getEventMoments().getFirst().getMomentPicturesUrls().isEmpty());
    }

    // ===== TC5 =====
    @Test
    void getEventMomentsFeed_empty() {

        when(eventMomentRepository.findAllByName(any(), any()))
                .thenReturn(Page.empty());

        EventMomentFeedResponse response =
                service.getEventMomentsFeed(0, 10, null);

        assertTrue(response.getEventMoments().isEmpty());
        assertFalse(response.isHasMore());
    }
}
