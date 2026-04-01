package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.event.request.SaveEventRequest;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    VolunteerRepository volunteerRepository;

    @Mock
    VolunteerSavedEventRepository volunteerSavedEventRepository;

    @Mock
    StorageService storageService;


    @Mock
    EventApplicationRepository eventApplicationRepository;

    @Mock
    CheckInLogRepository checkInLogRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    EventSessionService eventSessionService;

    @Mock
    NotificationService notificationService;

    @InjectMocks
    EventServiceImpl eventService;

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

    private SaveEventRequest validSaveEventRequest() {
        SaveEventRequest req = new SaveEventRequest();
        req.setEventId(eventId.toString());
        return req;
    }

    // ==== getEventFeeds ===================================
    // ===== TC1 =====
    @Test
    void getEventFeeds_search_success() {

        Event event = mockEvent();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.search(
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        EventFeedResponse response = eventService.getEventFeeds(
                0,
                10,
                false,
                "Sự kiện",
                "Hà Nội",
                LocalDate.of(2026, 3, 5),
                LocalDate.of(2026, 3, 10),
                null
        );

        assertEquals(1, response.getEvents().size());
        assertEquals("signed-url", response.getEvents().getFirst().getImageUrl());

        verify(eventRepository).search(
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    // ===== TC2 =====
    @Test
    void getEventFeeds_refresh_success() {

        Event event = mockEvent();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.refresh(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        EventFeedResponse response = eventService.getEventFeeds(
                0,
                10,
                true,
                null,
                null,
                null,
                null,
                null
        );

        assertEquals(1, response.getEvents().size());

        verify(eventRepository).refresh(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    // ===== TC3 =====
    @Test
    void getEventFeeds_event_without_images() {

        Event event = mockEvent();
        event.setImages(null);

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.searchWithActivitySubDomain(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        EventFeedResponse response = eventService.getEventFeeds(
                0,
                10,
                false,
                null,
                null,
                LocalDate.of(2026, 3, 5),
                null,
                List.of(Short.valueOf("1"), Short.valueOf("2"))
        );

        assertNull(response.getEvents().getFirst().getImageUrl());
    }


    // ===== TC4 =====
    @Test
    void getEventFeeds_should_return_null_next_page() {

        Event event = mockEvent();

        Page<Event> page = new PageImpl<>(
                List.of(event),
                PageRequest.of(0, 10),
                1 // total elements
        );

        when(eventRepository.search(
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        EventFeedResponse response = eventService.getEventFeeds(
                0,
                10,
                false,
                null,
                null,
                null,
                null,
                null
        );

        assertFalse(response.isHasMore());
        assertNull(response.getNextCursor());
    }

    // ==== getEventDetails ===================================
    // ===== TC1 =====
    @Test
    void getEventDetails_success() {

        Event event = mockEvent();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        EventDetailsResponse response = eventService.getEventDetails(eventId);

        assertEquals(eventId, response.getId());
        assertEquals("Charity Event", response.getName());
        assertEquals(2, response.getImageUrls().size());
        assertEquals("Volunteer Org", response.getOrgName());
        assertEquals("0901234567", response.getHostPhone());
        assertEquals("Education", response.getActivitySubDomain());

        verify(eventRepository).findById(eventId);
    }

    // ===== TC2 =====
    @Test
    void getEventDetails_event_not_exist() {

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.getEventDetails(eventId)
        );

        assertEquals(EventErrorCode.EVENT_NOT_EXISTED.getCode(), ex.getCode());

        verify(eventRepository).findById(eventId);
    }

    // ===== TC3 =====
    @Test
    void getEventDetails_event_without_images() {

        Event event = mockEvent();
        event.setImages(null);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        EventDetailsResponse response = eventService.getEventDetails(eventId);

        assertTrue(response.getImageUrls().isEmpty());
    }

    // ===== TC4 =====
    @Test
    void getEventDetails_null_host_and_org() {

        Event event = mockEvent();

        event.setHost(null);
        event.setOrganization(null);
        event.setActivitySubDomain(null);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        EventDetailsResponse response = eventService.getEventDetails(eventId);

        assertEquals("", response.getHostPhone());
        assertEquals("", response.getOrgName());
        assertEquals("", response.getActivitySubDomain());
    }

    // ==== saveEvent ===================================
    // ===== TC1 =====
    @Test
    void saveEvent_success() {

        SaveEventRequest request = validSaveEventRequest();

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volunteerId);

        Event event = new Event();
        event.setId(eventId);

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        eventService.saveEvent(request);

        verify(volunteerSavedEventRepository)
                .save(any(VolunteerSavedEvent.class));

        verify(volunteerRepository).findById(volunteerId);
        verify(eventRepository).findById(eventId);
    }

    // ===== TC2 =====
    @Test
    void saveEvent_fail_volunteer_not_exist() {

        SaveEventRequest request = validSaveEventRequest();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.saveEvent(request)
        );

        assertEquals(
                VolunteerErrorCode.VOLUNTEER_NOT_EXISTED.getCode(),
                ex.getCode()
        );

        verify(volunteerSavedEventRepository, never()).save(any());
    }

    // ===== TC3 =====
    @Test
    void saveEvent_fail_event_not_exist() {

        SaveEventRequest request = validSaveEventRequest();

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volunteerId);

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.saveEvent(request)
        );

        assertEquals(
                EventErrorCode.EVENT_NOT_EXISTED.getCode(),
                ex.getCode()
        );

        verify(volunteerSavedEventRepository, never()).save(any());
    }

    // ==== getEventDetailsByManager ===================================
    // ===== TC1 =====
    @Test
    void getEventDetailsByManager_success() {

        Event event = mockEvent();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        EventDetailsResponseForManager response =
                eventService.getEventDetailsByManager(eventId);

        assertEquals("Charity Event", response.getName());
        assertEquals(2, response.getImageUrls().size());
        assertEquals("Host A", response.getHostName());
        assertEquals(1, response.getEventSessions().size());

        verify(eventRepository).findById(eventId);
    }

    // ===== TC2 =====
    @Test
    void getEventDetailsByManager_event_not_exist() {

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.getEventDetailsByManager(eventId)
        );

        assertEquals(
                EventErrorCode.EVENT_NOT_EXISTED.getCode(),
                ex.getCode()
        );
    }

    // ===== TC3 =====
    @Test
    void getEventDetailsByManager_conflict_sessions_exist() {

        Event event = mockEvent();

        EventSession conflict = new EventSession();
        conflict.setId(UUID.randomUUID());
        conflict.setStartDateTime(OffsetDateTime.now());
        conflict.setEndDateTime(OffsetDateTime.now().plusHours(1));
        conflict.setExpectedVolAmount(3);
        conflict.setExpectedSerAmount(6);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        when(eventSessionService.findConflictSessionDateOfHost(
                any(),
                any(),
                anyList()
        )).thenReturn(List.of(conflict));

        EventDetailsResponseForManager response =
                eventService.getEventDetailsByManager(eventId);

        verify(eventSessionService).findConflictSessionDateOfHost(
                any(),
                any(),
                anyList()
        );

        assertEquals(1, response.getConflictSessions().size());
        assertFalse(response.getConflictSessions().isEmpty());
        assertTrue(response.getNote()
                .contains(EventErrorCode.DUPLICATE_HOSTED_DATE.getMessage()));
    }

    // ===== TC4 =====
    @Test
    void getEventDetailsByManager_null_activity_sub_domain() {

        Event event = mockEvent();

        event.setActivitySubDomain(null);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        EventDetailsResponseForManager response = eventService.getEventDetailsByManager(eventId);

        assertEquals("", response.getActivitySubDomain());
    }

//    // ===== TC4 =====
//    @Test
//    void getEventDetailsByManager_status_recruiting_should_return_dates() {
//
//        Event event = mockEvent();
//        event.setStatus(EEventStatus.RECRUITING);
//        event.setStartDate(LocalDate.now());
//        event.setRecruitmentEndDate(LocalDate.now().plusDays(5));
//
//        when(eventRepository.findById(eventId))
//                .thenReturn(Optional.of(event));
//
//        when(storageService.getSignedUrlAsync("img1"))
//                .thenReturn(CompletableFuture.completedFuture("url1"));
//
//        when(storageService.getSignedUrlAsync("img2"))
//                .thenReturn(CompletableFuture.completedFuture("url2"));
//
//        when(eventSessionService.findConflictSessionDateOfHost(
//                any(),
//                any(),
//                anyList()
//        )).thenReturn(Collections.emptyList());
//
//        EventDetailsResponseForManager response =
//                eventService.getEventDetailsByManager(eventId);
//
//        assertNotNull(response.getStartDate());
//        assertNotNull(response.getRecruitmentEndDate());
//    }
//
//    // ===== TC5 =====
//    @Test
//    void getEventDetailsByManager_status_submitted_should_return_dates() {
//
//        Event event = mockEvent();
//        event.setStatus(EEventStatus.SUBMITTED);
//        event.setStartDate(LocalDate.now());
//        event.setRecruitmentEndDate(LocalDate.now().plusDays(5));
//
//        when(eventRepository.findById(eventId))
//                .thenReturn(Optional.of(event));
//
//        when(storageService.getSignedUrlAsync("img1"))
//                .thenReturn(CompletableFuture.completedFuture("url1"));
//
//        when(storageService.getSignedUrlAsync("img2"))
//                .thenReturn(CompletableFuture.completedFuture("url2"));
//
//        when(eventSessionService.findConflictSessionDateOfHost(
//                any(),
//                any(),
//                anyList()
//        )).thenReturn(Collections.emptyList());
//
//        EventDetailsResponseForManager response =
//                eventService.getEventDetailsByManager(eventId);
//
//        assertNotNull(response.getStartDate());
//        assertNotNull(response.getRecruitmentEndDate());
//    }

    // ==== getEventDetailsBySystemAdmin ===================================
    // ===== TC1 =====
    @Test
    void getEventDetailsBySystemAdmin_success() {

        Event event = mockEvent();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        EventDetailsResponseForSystemAdmin response =
                eventService.getEventDetailsBySystemAdmin(eventId);

        assertEquals("Charity Event", response.getName());
        assertEquals(2, response.getImageUrls().size());
        assertEquals(1, response.getEventSessions().size());

        verify(eventRepository).findById(eventId);
    }

    // ===== TC2 =====
    @Test
    void getEventDetailsBySystemAdmin_event_not_exist() {

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.getEventDetailsBySystemAdmin(eventId)
        );

        assertEquals(
                EventErrorCode.EVENT_NOT_EXISTED.getCode(),
                ex.getCode()
        );
    }

    // ===== TC3 =====
    @Test
    void getEventDetailsBySystemAdmin_conflict_sessions_exist() {

        Event event = mockEvent();

        EventSession conflict = new EventSession();
        conflict.setId(UUID.randomUUID());
        conflict.setStartDateTime(OffsetDateTime.now());
        conflict.setEndDateTime(OffsetDateTime.now().plusHours(1));
        conflict.setExpectedVolAmount(3);
        conflict.setExpectedSerAmount(6);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        when(eventSessionService.findConflictSessionDateOfHost(
                any(),
                any(),
                anyList()
        )).thenReturn(List.of(conflict));

        EventDetailsResponseForSystemAdmin response =
                eventService.getEventDetailsBySystemAdmin(eventId);

        verify(eventSessionService).findConflictSessionDateOfHost(
                any(),
                any(),
                anyList()
        );

        assertEquals(1, response.getConflictSessions().size());
        assertFalse(response.getConflictSessions().isEmpty());
        assertTrue(response.getNote()
                .contains(EventErrorCode.DUPLICATE_HOSTED_DATE.getMessage()));
    }

    // ===== TC4 =====
    @Test
    void getEventDetailsBySystemAdmin_null_activity_sub_domain() {

        Event event = mockEvent();

        event.setActivitySubDomain(null);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        EventDetailsResponseForSystemAdmin response = eventService.getEventDetailsBySystemAdmin(eventId);

        assertEquals("", response.getActivitySubDomain());
    }

    // ==== getEventsByHost ===================================
    // ===== TC1 =====
    @Test
    void getEventsByHost_success() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        Event event = mockEvent();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.findEventsByHostId(
                eq(hostId),
                any(),
                any(),
                any()
        )).thenReturn(page);

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        Page<EventSimpleResponseForHost> response =
                eventService.getEventsByHost(0, 10, "Charity Event", "RECRUITING");

        assertEquals(1, response.getContent().size());
        assertEquals("signed-url",
                response.getContent().getFirst().getImageUrl());
    }

    // ===== TC2 =====
    @Test
    void getEventsByHost_no_events() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(eventRepository.findEventsByHostId(
                eq(hostId),
                any(),
                any(),
                any()
        )).thenReturn(emptyPage);

        Page<EventSimpleResponseForHost> response =
                eventService.getEventsByHost(0, 10, null, "RECRUITING");

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getTotalElements());
    }

    // ===== TC3 =====
    @Test
    void getEventsByHost_parse_null_and_without_image() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        Event event = mockEvent();
        event.setImages(null);

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.findEventsByHostId(
                eq(hostId),
                any(),
                any(),
                any()
        )).thenReturn(page);

        Page<EventSimpleResponseForHost> response =
                eventService.getEventsByHost(0, 10, null, null);

        assertEquals(1, response.getContent().size());
        assertNull(response.getContent().getFirst().getImageUrl());
    }

    // ==== getEventDetailsByHost ===================================
    // ===== TC1 =====
    @Test
    void getEventDetailsByHost_success() {

        Event event = mockEvent();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(storageService.getSignedUrlAsync("img1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("img2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        EventDetailsResponseForHost response =
                eventService.getEventDetailsByHost(eventId);

        assertEquals("Charity Event", response.getName());
        assertEquals(2, response.getImageUrls().size());
        assertEquals(1, response.getEventSessions().size());

        verify(eventRepository).findById(eventId);
    }

    // ===== TC2 =====
    @Test
    void getEventDetailsByHost_event_not_exist() {

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> eventService.getEventDetailsByHost(eventId)
        );

        assertEquals(
                EventErrorCode.EVENT_NOT_EXISTED.getCode(),
                ex.getCode()
        );
    }

    // ===== TC3 =====
    @Test
    void getEventDetailsByHost_null_activity_sub_domain_null_image() {

        Event event = mockEvent();
        event.setImages(null);

        event.setActivitySubDomain(null);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        EventDetailsResponseForHost response = eventService.getEventDetailsByHost(eventId);

        assertEquals("", response.getActivitySubDomain());
        assertEquals(new ArrayList<>(),response.getImageUrls());
    }

    // ==== announceVolunteersOfEvent ===================================
    private Event event(EEventStatus status) {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setStatus(status);
        return e;
    }

    private AnnounceVolunteerRequest req() {
        return new AnnounceVolunteerRequest();
    }

    // TC01
    @Test
    void announceVolunteer_upcoming_success() {

        Event event = event(EEventStatus.UPCOMING);

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        eventService.announceVolunteersOfEvent(event.getId(), req());

        verify(notificationService)
                .sendNotificationToVolunteersOfEvent(eq(event.getId()), any());
    }

    // TC02
    @Test
    void announceVolunteer_ongoing_success() {

        Event event = event(EEventStatus.ONGOING);

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        eventService.announceVolunteersOfEvent(event.getId(), req());

        verify(notificationService)
                .sendNotificationToVolunteersOfEvent(eq(event.getId()), any());
    }

    // TC03
    @Test
    void announceVolunteer_eventNotExist_shouldThrow() {

        when(eventRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> eventService.announceVolunteersOfEvent(UUID.randomUUID(), req()));
    }

    // TC04
    @Test
    void announceVolunteer_invalidStatus_shouldThrow() {

        Event event = event(EEventStatus.RECRUITING); // không hợp lệ

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> eventService.announceVolunteersOfEvent(event.getId(), req()));

        verifyNoInteractions(notificationService);
    }
}
