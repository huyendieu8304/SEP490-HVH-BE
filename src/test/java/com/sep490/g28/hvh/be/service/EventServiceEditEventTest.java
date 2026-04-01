package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventImagePayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventSessionPayload;
import com.sep490.g28.hvh.be.dto.event.request.CancelEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.EditEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.RejectEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.dto.event.response.EditEventResponse;
import com.sep490.g28.hvh.be.dto.event.response.UpdateEventResponse;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.ActivitySubDomainRepository;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.service.impl.EventServiceImpl;
import com.sep490.g28.hvh.be.util.GeoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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
    @Mock
    OrganizationService organizationService;

    @Mock
    EmailService emailService;

    @Mock
    private EventApplicationRepository eventApplicationRepository;

    @Mock
    private EventApplicationService eventApplicationService;

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

    private Event eventForApproveReject(UUID eventId) {

        Event event = new Event();
        event.setId(eventId);
        event.setStatus(EEventStatus.SUBMITTED);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(5));

        Host host = new Host();
        host.setId(UUID.randomUUID());
        event.setHost(host);

        event.setSessions(new ArrayList<>());
        event.getSessions().add(new EventSession());

        return event;
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

    private RejectEventRequest rejectEventRequest(){
        RejectEventRequest request = new RejectEventRequest();
        request.setReason("reason");
        return request;
    }

    private Event eventForUpdate(UUID id) {
        Event e = new Event();
        e.setId(id);
        e.setStatus(EEventStatus.RECRUITING);
        e.setDescription("old");
        e.setAutoApprove(false);
        e.setServingPlaceType(EServingPlaceType.HOSPITAL);
        e.setAddress("old addr");
        e.setDetailAddress("old detail");
        e.setCheckInAccuracyMeters(10.0);
        e.setCheckInLocation(GeoUtils.toPoint(10.0, 10.0));

        Organization org = new Organization();
        OrganizationManager mng = new OrganizationManager();
        mng.setId(UUID.randomUUID());
        org.setOrganizationManager(mng);
        e.setOrganization(org);

        e.setName("event");

        return e;
    }

    private Event eventForCancel(UUID id) {
        Event e = new Event();
        e.setId(id);
        e.setStatus(EEventStatus.RECRUITING);

        Organization org = new Organization();
        org.setName("org");

        OrganizationManager mng = new OrganizationManager();
        mng.setEmail("mng@mail.com");
        mng.setFullName("manager");
        org.setOrganizationManager(mng);

        e.setOrganization(org);

        Host host = new Host();
        host.setEmail("host@mail.com");
        host.setFullName("host");
        e.setHost(host);

        e.setName("event");

        return e;
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
    @Test
    void approveEventByManager_create_success() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(null);

        when(eventRepository.findById(any())).thenReturn(Optional.of(event));
        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.approveEventByManager(eventId);

        assertEquals(EEventStatus.APPROVED_BY_MNG, event.getStatus());
        verify(eventRepository).save(event);
        verify(notificationService)
                .sendEventCreateApprovedByOrgManagerNotification(event);
    }
    @Test
    void approveEventByManager_create_duplicateDate_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(List.of(new EventSession()));

        assertThrows(AppException.class,
                () -> service.approveEventByManager(eventId));
    }

    @Test
    void approveEventByManager_create_recruitmentExpired_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(null);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> service.approveEventByManager(eventId));
    }

    @Test
    void approveEventByManager_updateCritical_success() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(true);

        UpdateEventSessionPayload sessionPayload = new UpdateEventSessionPayload();
        sessionPayload.setStartDateTime(OffsetDateTime.now().plusDays(10));
        sessionPayload.setEndDateTime(OffsetDateTime.now().plusDays(10).plusHours(2));

        UpdateEventPayload payload = new UpdateEventPayload();
        payload.setRecruitmentEndDate(LocalDate.now().plusDays(5));
        payload.setEventSessions(List.of(sessionPayload));

        // thêm để cover branch khác
        payload.setAddress("addr");
        payload.setDetailAddress("detail");
        payload.setStartDate(LocalDate.now().plusDays(10));
        payload.setEndDate(LocalDate.now().plusDays(12));
        payload.setCheckInLocationLat(10.0);
        payload.setCheckInLocationLng(20.0);
        payload.setCheckInLocationAccuracyMeters((double)500);

        event.setUpdateEventPayload(payload);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.approveEventByManager(eventId);

        assertEquals(EEventStatus.APPROVED_BY_MNG, event.getStatus());
        verify(notificationService)
                .sendEventUpdateCriticalApprovedByOrgManagerNotification(
                        eq(event.getHost().getId()), eq(event));
    }

    @Test
    void approveEventByManager_updateCritical_conflict_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(true);

        UpdateEventPayload payload = new UpdateEventPayload();
        payload.setEventSessions(List.of(new UpdateEventSessionPayload()));
        event.setUpdateEventPayload(payload);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(List.of(new EventSession()));

        assertThrows(AppException.class,
                () -> service.approveEventByManager(eventId));
    }

    @Test
    void approveEventByManager_updateNonCritical_success() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setUpdateCritical(false);

        UpdateEventPayload payload = new UpdateEventPayload();
        payload.setDescription("new desc");

        payload.setAutoApprove(true);
        payload.setServingPlaceType(EServingPlaceType.HOSPITAL);

        payload.setEventImages(List.of(new UpdateEventImagePayload()));

        event.setUpdateEventPayload(payload);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventApplicationRepository.getPendingAndApprovedApplications(any()))
                .thenReturn(Collections.emptyList());

        service.approveEventByManager(eventId);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());
        assertNull(event.getUpdateCritical());
        assertNull(event.getUpdateEventPayload());

        verify(notificationService)
                .sendEventUpdateNonCriticalApprovedByOrgManagerNotification(
                        eq(event.getHost().getId()), eq(event));
    }

    @Test
    void approveEventByAdmin_create_success() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.approveEventByAdmin(eventId);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());
        verify(notificationService)
                .sendEventCreateApprovedByAdminNotification(event);
    }

    @Test
    void approveEventByAdmin_create_duplicate_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventSessionService.findConflictSessionDateOfHost(any(), any(), any()))
                .thenReturn(List.of(new EventSession()));

        assertThrows(AppException.class,
                () -> service.approveEventByAdmin(eventId));
    }

    @Test
    void approveEventByAdmin_updateCritical_success() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);

        UpdateEventPayload payload = new UpdateEventPayload();
        payload.setRecruitmentEndDate(LocalDate.now().plusDays(5));

        payload.setEventSessions(List.of(new UpdateEventSessionPayload()));
        payload.setEventImages(List.of(new UpdateEventImagePayload()));

        event.setUpdateEventPayload(payload);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(eventApplicationService.cancelAllApplicationsOfEvent(event))
                .thenReturn(Collections.emptyList());

        service.approveEventByAdmin(eventId);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());
        verify(notificationService)
                .sendEventUpdateCriticalApprovedByAdminNotification(event);
    }

    @Test
    void approveEventByAdmin_invalidStatus_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        event.setStatus(EEventStatus.SUBMITTED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> service.approveEventByAdmin(eventId));
    }

    @Test
    void rejectEventByManager_invalidStatus_shouldThrow() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> service.rejectEventByManager(eventId, request));
    }

    @Test
    void rejectEventByManager_create_success() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setUpdateCritical(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, request);

        assertEquals(EEventStatus.REJECTED_BY_MNG, event.getStatus());
        verify(notificationService)
                .sendEventCreateRejectedByOrgManagerNotification(event, request.getReason());
    }

    @Test
    void rejectEventByManager_update_recruiting() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(5));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, request);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());
        assertNull(event.getUpdateCritical());
        assertNull(event.getUpdateEventPayload());
    }

    @Test
    void rejectEventByManager_update_ongoing() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(5));
        event.setStartDate(LocalDate.now().minusDays(1));
        event.setEndDate(LocalDate.now().plusDays(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, request);

        assertEquals(EEventStatus.ONGOING, event.getStatus());
    }

    @Test
    void rejectEventByManager_update_completed() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(10));
        event.setStartDate(LocalDate.now().minusDays(5));
        event.setEndDate(LocalDate.now().minusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, request);

        assertEquals(EEventStatus.COMPLETED, event.getStatus());
    }

    @Test
    void rejectEventByManager_update_shouldSendNotification() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByManager(eventId, request);

        verify(notificationService)
                .sendEventUpdateRejectedByOrgManagerNotification(
                        eq(event.getHost().getId()), eq(event));
    }


    @Test
    void rejectEventByAdmin_invalidStatus_shouldThrow() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.SUBMITTED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThrows(AppException.class,
                () -> service.rejectEventByAdmin(eventId, request));
    }

    @Test
    void rejectEventByAdmin_create_success() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        assertEquals(EEventStatus.REJECTED_BY_AD, event.getStatus());
        verify(notificationService)
                .sendEventCreateRejectedByAdminNotification(event, request.getReason());
    }

    @Test
    void rejectEventByAdmin_update_recruiting() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        assertEquals(EEventStatus.RECRUITING, event.getStatus());
    }

    @Test
    void rejectEventByAdmin_update_upcoming() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(1));
        event.setStartDate(LocalDate.now().plusDays(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        assertEquals(EEventStatus.UPCOMING, event.getStatus());
    }

    @Test
    void rejectEventByAdmin_update_completed() {
        UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(10));
        event.setStartDate(LocalDate.now().minusDays(5));
        event.setEndDate(LocalDate.now().minusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        assertEquals(EEventStatus.COMPLETED, event.getStatus());
    }
    @Test
    void rejectEventByAdmin_update_ongoing() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().minusDays(5));
        event.setStartDate(LocalDate.now().minusDays(1));
        event.setEndDate(LocalDate.now().plusDays(2));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        assertEquals(EEventStatus.ONGOING, event.getStatus());
    }



    @Test
    void rejectEventByAdmin_update_shouldSendNotification() {
                UUID eventId = UUID.randomUUID();
        Event event = eventForApproveReject(eventId);
        RejectEventRequest request = rejectEventRequest();
        event.setStatus(EEventStatus.APPROVED_BY_MNG);
        event.setUpdateCritical(true);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(1));

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.rejectEventByAdmin(eventId, request);

        verify(notificationService)
                .sendEventUpdateCriticalRejectedByAdminNotification(event);
    }

    // ===== updateEvent
    // TC1: event not exist =====
    @Test
    void updateEvent_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.updateEvent(id, new UpdateEventRequest()));
    }

    // TC2: status invalid =====
    @Test
    void updateEvent_invalidStatus_shouldThrow() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);
        e.setStatus(EEventStatus.COMPLETED);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));

        assertThrows(AppException.class,
                () -> service.updateEvent(id, new UpdateEventRequest()));
    }

    // TC3: no changes =====
    @Test
    void updateEvent_noChanges_shouldThrow() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        UpdateEventRequest req = new UpdateEventRequest();

        assertThrows(AppException.class,
                () -> service.updateEvent(id, req));
    }

    // TC4: only non-critical =====
    @Test
    void updateEvent_nonCriticalChange_success() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setDescription("new desc"); // non-critical

        UpdateEventResponse res = service.updateEvent(id, req);

        assertEquals(EEventStatus.SUBMITTED, e.getStatus());
        assertFalse(e.getUpdateCritical());
        assertNotNull(e.getUpdateEventPayload());

        verify(notificationService).sentEventUpdatedByHostNotification(
                any(), eq(id), eq(e.getName())
        );
    }

    // TC5: critical change (address) =====
    @Test
    void updateEvent_criticalChange_success() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setDescription("new desc");
        req.setAutoApprove(false);
        req.setAddress("Phường Ba Đình");
        req.setDetailAddress("new address");
        req.setServingPlaceType(EServingPlaceType.HOSPITAL);

        service.updateEvent(id, req);

        assertTrue(e.getUpdateCritical());
        assertEquals(EEventStatus.SUBMITTED, e.getStatus());
    }

    // TC6: update datetime =====
    @Test
    void updateEvent_updateDateTime_shouldMarkCritical() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(true);

        UpdateEventRequest req = new UpdateEventRequest();

        service.updateEvent(id, req);

        assertTrue(e.getUpdateCritical());
        assertEquals(EEventStatus.SUBMITTED, e.getStatus());
    }

    // TC7: update images =====
    @Test
    void updateEvent_updateImages_shouldReturnUploadUrls() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        when(eventImageService.resolveUpdateEventImagesPayload(any(), any(), any()))
                .thenReturn(List.of("url1", "url2"));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setUpdateImages(List.of(new EditEventImageRequest()));

        UpdateEventResponse res = service.updateEvent(id, req);

        assertEquals(2, res.getUploadUrls().size());
    }

    // TC8: checkin location change =====
    @Test
    void updateEvent_checkinLocationChange_shouldCritical() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setCheckInLocationLat(20.0);
        req.setCheckInLocationLng(20.0);

        service.updateEvent(id, req);

        assertTrue(e.getUpdateCritical());
    }

    // TC9: accuracy change =====
    @Test
    void updateEvent_accuracyChange_shouldCritical() {
        UUID id = UUID.randomUUID();
        Event e = eventForUpdate(id);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventSessionService.checkAndResolveUpdateEventDateTime(any(), any(), any()))
                .thenReturn(false);

        UpdateEventRequest req = new UpdateEventRequest();
        req.setCheckInLocationAccuracyMeters(50);

        service.updateEvent(id, req);

        assertTrue(e.getUpdateCritical());
    }


    // ===== cancelEventByHost

    // TC1: not found
    @Test
    void cancelEventByHost_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.cancelEventByHost(id, new CancelEventRequest()));
    }

    // TC2: status invalid
    @Test
    void cancelEventByHost_invalidStatus_shouldThrow() {
        UUID id = UUID.randomUUID();
        Event e = eventForCancel(id);
        e.setStatus(EEventStatus.COMPLETED);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));

        assertThrows(AppException.class,
                () -> service.cancelEventByHost(id, new CancelEventRequest()));
    }

    // TC3: success
    @Test
    void cancelEventByHost_success() {
        UUID id = UUID.randomUUID();
        Event e = eventForCancel(id);

        CancelEventRequest req = new CancelEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventApplicationService.cancelAllApplicationsOfEvent(e))
                .thenReturn(Collections.emptyList());

        service.cancelEventByHost(id, req);

        // status
        assertEquals(EEventStatus.CANCELLED, e.getStatus());

        // verify flow
        verify(eventRepository).save(e);
        verify(organizationService).deductCreditHourOfOrganization(e.getOrganization(), 3);
        verify(eventApplicationService).cancelAllApplicationsOfEvent(e);

        verify(notificationService)
                .sentEventCancelledByHostNotification(any(), eq(e.getName()), eq("reason"));

        verify(emailService)
                .sendEventCancelledByHostEmail(
                        eq(e.getOrganization().getOrganizationManager().getEmail()),
                        eq(e.getOrganization().getOrganizationManager().getFullName()),
                        eq(e.getOrganization().getName()),
                        eq(e.getName()),
                        eq(e.getHost().getFullName()),
                        eq(e.getHost().getEmail()),
                        eq("reason")
                );
    }

    // ===== cancelEventByAdmin

    // TC4: not found
    @Test
    void cancelEventByAdmin_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.cancelEventByAdmin(id, new CancelEventRequest()));
    }

    // TC5: status invalid
    @Test
    void cancelEventByAdmin_invalidStatus_shouldThrow() {
        UUID id = UUID.randomUUID();
        Event e = eventForCancel(id);
        e.setStatus(EEventStatus.COMPLETED);

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));

        assertThrows(AppException.class,
                () -> service.cancelEventByAdmin(id, new CancelEventRequest()));
    }

    // TC6: success
    @Test
    void cancelEventByAdmin_success() {
        UUID id = UUID.randomUUID();
        Event e = eventForCancel(id);

        CancelEventRequest req = new CancelEventRequest();
        req.setReason("reason");

        when(eventRepository.findById(id)).thenReturn(Optional.of(e));
        when(eventApplicationService.cancelAllApplicationsOfEvent(e))
                .thenReturn(Collections.emptyList());

        service.cancelEventByAdmin(id, req);

        // status
        assertEquals(EEventStatus.CANCELLED, e.getStatus());

        // verify flow
        verify(eventRepository).save(e);
        verify(organizationService).deductCreditHourOfOrganization(e.getOrganization(), 3);
        verify(eventApplicationService).cancelAllApplicationsOfEvent(e);

        verify(notificationService)
                .sentEventCancelledByAdminNotification(any(), eq(e.getName()), eq("reason"));

        verify(emailService)
                .sendEventCancelledByAdminEmail(
                        eq(e.getOrganization().getOrganizationManager().getEmail()),
                        eq(e.getOrganization().getOrganizationManager().getFullName()),
                        eq(e.getOrganization().getName()),
                        eq(e.getName()),
                        eq("reason")
                );
    }

}
