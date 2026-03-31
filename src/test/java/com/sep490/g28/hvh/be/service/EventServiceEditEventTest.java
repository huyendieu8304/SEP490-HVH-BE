package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.event.request.EditEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.RejectEventRequest;
import com.sep490.g28.hvh.be.dto.event.response.EditEventResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.repository.ActivitySubDomainRepository;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceEditEventTest {

    @InjectMocks
    EventServiceImpl service;

    @Mock
    HostRepository hostRepository;
    @Mock
    ActivitySubDomainRepository activitySubDomainRepository;
    @Mock
    EventRepository eventRepository;
    @Mock
    CurrentUserProvider currentUserProvider;
    @Mock
    EventImageService eventImageService;
    @Mock
    EventSessionService eventSessionService;
    @Mock
    NotificationService notificationService;

    @BeforeEach
    void setup() {

        Host host = new Host();
        host.setOrganization(new Organization());

        lenient().when(currentUserProvider.getId())
                .thenReturn(UUID.randomUUID());

        lenient().when(hostRepository.getReferenceById(any()))
                .thenReturn(host);
    }

    private Event event() {
        Event e = new Event();
        e.setId(UUID.randomUUID());

        Host host = new Host();
        host.setId(UUID.randomUUID());

        e.setHost(host);

        e.setStatus(EEventStatus.EDITING);
        e.setSessions(new ArrayList<>());
        e.setImages(new ArrayList<>());
        return e;
    }

    private EditEventRequest request() {
        EditEventRequest r = new EditEventRequest();

        r.setActivitySubDomainId((short) 1);

        r.setName("Test event");

        r.setDescription("Test description");

        r.setAddress("Hà Nội");

        r.setDetailAddress("Tòa nhà Lestat Lioncourt");

        r.setAutoApprove(true);

        r.setServingActivity(true);

        r.setServedTarget(EServedTarget.ELDERLY);

        r.setServingPlaceType(EServingPlaceType.HOSPITAL);

        r.setRecruitmentEndDate(LocalDate.now().plusDays(20));

        r.setEventSessions(List.of());

        r.setUpdateImages(List.of());

        r.setCheckInPlaceLat(21.0285);

        r.setCheckInPlaceLng(105.8542);

        r.setCheckInPlaceAccuracyMeters(500);

        return r;
    }

    private ActivitySubDomain activitySubDomain() {
        ActivityDomain domain = new ActivityDomain();
        domain.setSpecialSessionMaxTime((short)4);

        ActivitySubDomain sub = new ActivitySubDomain();
        sub.setActivityDomain(domain);
        return sub;
    }

    // ==== draftEvent ===================================
    // TC01
    @Test
    void draftEvent_newEvent_shouldCreate() {

        EditEventRequest req = request();
        req.setEventId(null);

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(eventImageService.addEventImages(any(), any()))
                .thenReturn(List.of());

        EditEventResponse res = service.draftEvent(req);

        verify(eventRepository).save(any());
    }

    // TC02
    @Test
    void draftEvent_existingEvent_shouldEdit() {

        Event event = event();

        EditEventRequest req = request();
        req.setEventId(event.getId());

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventImageService.updateEventImages(any(), any()))
                .thenReturn(List.of());

        service.draftEvent(req);

        verify(eventRepository).save(any());
    }

    // TC03
    @Test
    void draftEvent_eventNotFound_shouldThrow() {

        EditEventRequest req = request();
        req.setEventId(UUID.randomUUID());

        when(eventRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.draftEvent(req));
    }

    // ==== draftEvent ===================================
    // TC01
    @Test
    void submitEvent_new_shouldSendNotification() {

        EditEventRequest req = request();

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(eventImageService.addEventImages(any(), any()))
                .thenReturn(List.of());

        service.submitEvent(req);

        verify(notificationService)
                .sendEventCreatedNotification(any(), any());
    }

    // TC03
    @Test
    void submitEvent_eventNotFound_shouldThrow() {

        EditEventRequest req = request();
        req.setEventId(UUID.randomUUID());

        when(eventRepository.findById(req.getEventId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> service.submitEvent(req)
        );

        assertEquals(EventErrorCode.EVENT_NOT_EXISTED.getCode(), ex.getCode());
    }

    // TC02
    @Test
    void submitEvent_existing_shouldEditAndNotify() {

        Event event = event();

        EditEventRequest req = request();
        req.setEventId(event.getId());

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventImageService.updateEventImages(any(), any()))
                .thenReturn(List.of());

        service.submitEvent(req);

        verify(notificationService)
                .sendEventCreatedNotification(any(), any());
    }

    // ==== createEvent ===================================
    // TC01
    @Test
    void createEvent_success_shouldSaveEvent() {

        EditEventRequest req = request();

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(eventImageService.addEventImages(any(), any()))
                .thenReturn(List.of());

        EditEventResponse res = service.draftEvent(req);

        verify(eventRepository).save(any());
    }

    // TC02
    @Test
    void createEvent_subdomainNotExist_shouldThrow() {

        EditEventRequest req = request();

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.draftEvent(req));
    }

    // TC03
    @Test
    void createEvent_specialSessionMaxTimeNull_shouldUseDefault4() {

        EditEventRequest req = request();

        ActivityDomain domain = new ActivityDomain();
        domain.setSpecialSessionMaxTime(null);

        ActivitySubDomain sub = new ActivitySubDomain();
        sub.setActivityDomain(domain);

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(sub));

        when(eventRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(eventImageService.addEventImages(any(), any()))
                .thenReturn(List.of());

        service.draftEvent(req);

        verify(eventSessionService).addEventSessionsForCreateEvent(
                any(),
                eq(req.getEventSessions())
        );
    }

    // ==== editEvent ===================================
    // TC01
    @Test
    void editEvent_success_shouldSave() {

        Event event = event();
        event.setStatus(EEventStatus.EDITING);

        EditEventRequest req = request();
        req.setEventId(event.getId());

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        when(activitySubDomainRepository.findById(any()))
                .thenReturn(Optional.of(activitySubDomain()));

        when(eventImageService.updateEventImages(any(), any()))
                .thenReturn(List.of());

        service.draftEvent(req);

        verify(eventRepository).save(event);
    }

    // TC02
    @Test
    void editEvent_notEditable_shouldThrow() {

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        EditEventRequest req = request();
        req.setEventId(event.getId());

        when(eventRepository.findById(event.getId()))
                .thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> service.draftEvent(req));
    }

    // ==== approveEventByManager ===================================
    // TC01
    @Test
    void approveEvent_success_shouldApproveAndNotify() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(List.of());

        service.approveEventByManager(eventId);

        assertEquals(EEventStatus.APPROVED_BY_MNG, event.getStatus());

        verify(eventRepository).save(event);

        verify(notificationService)
                .sendEventCreateApprovedByOrgManagerNotification(event);
    }

    // TC02
    @Test
    void approveEvent_eventNotExist_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> service.approveEventByManager(eventId)
        );
    }

    // TC03
    @Test
    void approveEvent_invalidStatus_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                AppException.class,
                () -> service.approveEventByManager(eventId)
        );
    }

    // TC04
    @Test
    void approveEvent_hostConflict_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        EventSession conflict = new EventSession();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(List.of(conflict));

        assertThrows(
                AppException.class,
                () -> service.approveEventByManager(eventId)
        );
    }

    // ==== rejectEventByManager ===================================
    // TC01
    @Test
    void rejectEvent_success_shouldRejectAndNotify() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("invalid plan");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, req);

        assertEquals(EEventStatus.REJECTED_BY_MNG, event.getStatus());

        verify(eventRepository).save(event);

        verify(notificationService)
                .sendEventCreateRejectedByOrgManagerNotification(event, req.getReason());
    }

    // TC02
    @Test
    void rejectEvent_eventNotExist_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> service.rejectEventByManager(eventId, req)
        );
    }

    // TC03
    @Test
    void rejectEvent_invalidStatus_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                AppException.class,
                () -> service.rejectEventByManager(eventId, req)
        );
    }

    // ==== approveEventByAdmin ===================================
    // TC01
    @Test
    void approveEventByAdmin_success_shouldApproveAndNotify() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        service.approveEventByAdmin(eventId);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());

        verify(eventRepository).save(event);

        verify(notificationService)
                .sendEventCreateApprovedByAdminNotification(event);
    }

    // TC02
    @Test
    void approveEventByAdmin_eventNotExist_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> service.approveEventByAdmin(eventId)
        );
    }

    // TC03
    @Test
    void approveEventByAdmin_invalidStatus_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                AppException.class,
                () -> service.approveEventByAdmin(eventId)
        );
    }

    // ==== rejectEventByAdmin ===================================
    // TC01
    @Test
    void rejectEventByAdmin_success_shouldRejectAndNotify() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("invalid plan");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, req);

        assertEquals(EEventStatus.REJECTED_BY_AD, event.getStatus());

        verify(eventRepository).save(event);

        verify(notificationService)
                .sendEventCreateRejectedByAdminNotification(event, req.getReason());
    }

    // TC02
    @Test
    void rejectEventByAdmin_eventNotExist_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> service.rejectEventByAdmin(eventId, req)
        );
    }

    // TC03
    @Test
    void rejectEventByAdmin_invalidStatus_shouldThrow() {

        UUID eventId = UUID.randomUUID();

        Event event = event();
        event.setStatus(EEventStatus.SUBMITTED);

        RejectEventRequest req = new RejectEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(eventId))
                .thenReturn(Optional.of(event));

        assertThrows(
                AppException.class,
                () -> service.rejectEventByAdmin(eventId, req)
        );
    }

}
