package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.impl.EventSessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventSessionServiceTest {

    @Mock
    private EventSessionRepository eventSessionRepository;

    @Spy
    @InjectMocks
    private EventSessionServiceImpl service;

    private Event event;
    private EventSession existing;


    Event mockEventForCreate() {
        Event e = new Event();
        e.setId(UUID.randomUUID());

        ActivityDomain domain = new ActivityDomain();
        domain.setSpecialSessionMaxTime((short) 4);

        ActivitySubDomain sub = new ActivitySubDomain();
        sub.setActivityDomain(domain);

        e.setActivitySubDomain(sub);
        Host host = new Host();
        host.setId(UUID.randomUUID());
        e.setHost(host);

        e.setRecruitmentEndDate(LocalDate.now().plusDays(5));
        return e;
    }

    Event mockEventForUpdate(){
        event = new Event();
        event.setId(UUID.randomUUID());

        Host host = new Host();
        host.setId(UUID.randomUUID());
        event.setHost(host);

        ActivityDomain domain = new ActivityDomain();
        domain.setSpecialSessionMaxTime((short) 4);

        ActivitySubDomain sub = new ActivitySubDomain();
        sub.setActivityDomain(domain);
        event.setStartDate(LocalDate.now().plusDays(15));

        event.setActivitySubDomain(sub);
        event.setRecruitmentEndDate(LocalDate.now().plusDays(10));

        existing = session(OffsetDateTime.now().plusDays(15));
        event.setSessions(new ArrayList<>(List.of(existing)));

        return event;
    }


    private EditEventSessionRequest req(EUpdateAction action, OffsetDateTime start, OffsetDateTime end) {
        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(action);
        r.setStartDateTime(start);
        r.setEndDateTime(end);
        r.setExpectedVolAmount(100);
        r.setExpectedSerAmount(200);
        return r;
    }

    private EventSession session(OffsetDateTime start) {
        EventSession s = new EventSession();
        s.setId(UUID.randomUUID());
        s.setStartDateTime(start);
        s.setEndDateTime(start.plusHours(2));
        s.setExpectedVolAmount(100);
        s.setExpectedSerAmount(200);
        s.setApprovedApplicationCount(0);
        return s;
    }

    EditEventSessionRequest req(EUpdateAction action) {
        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(action);
        r.setExpectedSerAmount(1);
        r.setExpectedVolAmount(1);
        return r;
    }

    // ==== addEventSessionForCreateEvent ===================================
    // TC01
    @Test
    void nullRequest_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, null));
    }

    // TC02
    @Test
    void emptyRequest_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of()));
    }

    // TC03
    @Test
    void noAddAction_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        EditEventSessionRequest r = req(EUpdateAction.EDIT);

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC04
    @Test
    void durationTooLong_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime start = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r = req(EUpdateAction.ADD);
        r.setStartDateTime(start);
        r.setEndDateTime(start.plusHours(10)); // > 4h

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC05
    @Test
    void duplicateSessionDay_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime start = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r1 = req(EUpdateAction.ADD);
        r1.setStartDateTime(start);
        r1.setEndDateTime(start.plusHours(2));

        EditEventSessionRequest r2 = req(EUpdateAction.ADD);
        r2.setStartDateTime(start.plusHours(3)); // cùng ngày
        r2.setEndDateTime(start.plusHours(5));

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r1, r2)));
    }

    // TC06
    @Test
    void conflictHost_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime date = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r = req(EUpdateAction.ADD);
        r.setStartDateTime(date);
        r.setEndDateTime(date.plusHours(2));

        EventSession exist = new EventSession();
        exist.setStartDateTime(date); // trùng ngày

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(List.of(exist));

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC07
    @Test
    void startDateTooSoon_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime date = OffsetDateTime.now().plusDays(5);

        EditEventSessionRequest r = req(EUpdateAction.ADD);
        r.setStartDateTime(date);
        r.setEndDateTime(date.plusHours(2));

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC08
    @Test
    void invalidRecruitmentEndDate_shouldThrow() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime date = OffsetDateTime.now().plusDays(20);

        event.setRecruitmentEndDate(date.toLocalDate().minusDays(1)); // sai

        EditEventSessionRequest r = req(EUpdateAction.ADD);
        r.setStartDateTime(date);
        r.setEndDateTime(date.plusHours(2));

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC09
    @Test
    void validSingleSession_shouldSetCorrect() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime date = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r = req(EUpdateAction.ADD);
        r.setStartDateTime(date);
        r.setEndDateTime(date.plusHours(2));

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.addEventSessionsForCreateEvent(event, List.of(r));

        assertEquals(1, event.getSessions().size());
        assertEquals(date.toLocalDate(), event.getStartDate());
        assertEquals(date.toLocalDate(), event.getEndDate());
    }

    // TC10
    @Test
    void validMultipleSessions_shouldResolveMinMaxDate() {
        event = mockEventForCreate(); // gọi factory method
        OffsetDateTime d1 = OffsetDateTime.now().plusDays(20);
        OffsetDateTime d2 = OffsetDateTime.now().plusDays(25);

        EditEventSessionRequest r1 = req(EUpdateAction.ADD);
        r1.setStartDateTime(d1);
        r1.setEndDateTime(d1.plusHours(2));

        EditEventSessionRequest r2 = req(EUpdateAction.ADD);
        r2.setStartDateTime(d2);
        r2.setEndDateTime(d2.plusHours(2));

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.addEventSessionsForCreateEvent(event, List.of(r1, r2));

        assertEquals(d1.toLocalDate(), event.getStartDate());
        assertEquals(d2.toLocalDate(), event.getEndDate());
    }


    @Test
    void updateEventSessions_editExistingSession_shouldUpdateAndResolveDate() {
        event = mockEventForUpdate();
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(EUpdateAction.EDIT);
        r.setEventSessionId(existing.getId());
        r.setStartDateTime(newDate);
        r.setEndDateTime(newDate.plusHours(2));
        r.setExpectedVolAmount(100);
        r.setExpectedSerAmount(200);

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.updateEventSessions(event, List.of(r));

        assertEquals(1, event.getSessions().size());
        assertEquals(newDate.toLocalDate(), event.getStartDate());
        assertEquals(newDate.toLocalDate(), event.getEndDate());
    }

    @Test
    void updateEventSessions_addSession_shouldKeepOldAndResolveMinMax() {
        event = mockEventForUpdate();
        OffsetDateTime newDate = OffsetDateTime.now().plusDays(25);

        EditEventSessionRequest r = req(EUpdateAction.ADD, newDate, newDate.plusHours(2));
        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.updateEventSessions(event, List.of(r));

        assertEquals(2, event.getSessions().size());

        LocalDate expectedStart = existing.getStartDateTime().toLocalDate();
        LocalDate expectedEnd = newDate.toLocalDate();

        assertEquals(expectedStart, event.getStartDate());
        assertEquals(expectedEnd, event.getEndDate());
    }

    @Test
    void updateEventSessions_removeOneOfMultiple_shouldPass() {
        event = mockEventForUpdate();
        EventSession s2 = session(OffsetDateTime.now().plusDays(20));
        event.getSessions().add(s2);

        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        r.setEventSessionId(existing.getId());

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.updateEventSessions(event, List.of(r));

        assertEquals(1, event.getSessions().size());
    }

    @Test
    void updateEventSessions_removeAllSessions_shouldThrow() {
        event = mockEventForUpdate();
        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        r.setEventSessionId(existing.getId());

        assertThrows(AppException.class,
                () -> service.updateEventSessions(event, List.of(r)));
    }

    @Test
    void updateEventSessions_addAndEdit_shouldWorkTogether() {
        event = mockEventForUpdate();
        OffsetDateTime d1 = OffsetDateTime.now().plusDays(20);
        OffsetDateTime d2 = OffsetDateTime.now().plusDays(25);

        EditEventSessionRequest edit = new EditEventSessionRequest();
        edit.setUpdateAction(EUpdateAction.EDIT);
        edit.setEventSessionId(existing.getId());
        edit.setStartDateTime(d1);
        edit.setEndDateTime(d1.plusHours(2));
        edit.setExpectedVolAmount(100);
        edit.setExpectedSerAmount(200);

        EditEventSessionRequest add = req(EUpdateAction.ADD, d2, d2.plusHours(2));

        when(eventSessionRepository.findByHostExcludingEvent(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        service.updateEventSessions(event, List.of(edit, add));

        assertEquals(2, event.getSessions().size());
        assertEquals(d1.toLocalDate(), event.getStartDate());
        assertEquals(d2.toLocalDate(), event.getEndDate());
    }

    @Test
    void updateEventSessions_sessionTooLong_shouldThrow() {
        event = mockEventForUpdate();
        OffsetDateTime d = OffsetDateTime.now().plusDays(20);

        EditEventSessionRequest r = req(EUpdateAction.ADD, d, d.plusHours(10));

        assertThrows(AppException.class,
                () -> service.updateEventSessions(event, List.of(r)));
    }

    @Test
    void updateEventSessions_conflictDate_shouldThrow() {
        event = mockEventForUpdate();
        OffsetDateTime d = existing.getStartDateTime();

        EditEventSessionRequest r = req(EUpdateAction.ADD, d, d.plusHours(2));

        r.setExpectedVolAmount(100);
        r.setExpectedSerAmount(200);

        EventSession conflict = session(d);
        conflict.setStartDateTime(d);

        assertThrows(AppException.class,
                () -> service.updateEventSessions(event, List.of(r)));
    }

    // ====== checkAndResolveUpdateEventDateTime ======
    @Test
    void checkAndResolveUpdateEventDateTime_noUpdate_shouldReturnFalse() {
                event = mockEventForUpdate();

        UpdateEventRequest req = new UpdateEventRequest();
        UpdateEventPayload payload = new UpdateEventPayload();

        boolean result = service.checkAndResolveUpdateEventDateTime(event, req, payload);

        assertFalse(result);
    }

    @Test
    void checkAndResolveUpdateEventDateTime_updateRecruitmentDate_shouldSetPayload() {
                event = mockEventForUpdate();

        UpdateEventRequest req = new UpdateEventRequest();
        LocalDate newDate = event.getStartDate().minusDays(3);
        req.setRecruitmentEndDate(newDate);

        UpdateEventPayload payload = new UpdateEventPayload();

        boolean result = service.checkAndResolveUpdateEventDateTime(event, req, payload);

        assertTrue(result);
        assertEquals(newDate, payload.getRecruitmentEndDate());
    }

    @Test
    void checkAndResolveUpdateEventDateTime_recruitmentDateInvalid_shouldThrow() {
                event = mockEventForUpdate();

        UpdateEventRequest req = new UpdateEventRequest();

        // violate: recruitmentEndDate > startDate - 3
        LocalDate invalid = event.getStartDate().minusDays(1);
        req.setRecruitmentEndDate(invalid);

        UpdateEventPayload payload = new UpdateEventPayload();

        assertThrows(AppException.class,
                () -> service.checkAndResolveUpdateEventDateTime(event, req, payload));
    }

    @Test
    void checkAndResolveUpdateEventDateTime_updateSessions_shouldResolvePayload() {
                event = mockEventForUpdate();

        OffsetDateTime d = OffsetDateTime.now().plusDays(25);

        EditEventSessionRequest add = req(EUpdateAction.ADD, d, d.plusHours(2));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setEventSessions(List.of(add));

        UpdateEventPayload payload = new UpdateEventPayload();

        boolean result = service.checkAndResolveUpdateEventDateTime(event, req, payload);

        assertTrue(result);
        assertEquals(2, payload.getEventSessions().size());

        assertEquals(existing.getStartDateTime().toLocalDate(), payload.getStartDate());
    }

    @Test
    void checkAndResolveUpdateEventDateTime_updateSessions_conflict_shouldThrow() {
                event = mockEventForUpdate();

        OffsetDateTime d = existing.getStartDateTime();

        EditEventSessionRequest add = req(EUpdateAction.ADD, d, d.plusHours(2));

        doReturn(List.of(new EventSession()))
                .when(service)
                .findConflictSessionDateOfHost(any(), any(), any());

        UpdateEventRequest req = new UpdateEventRequest();
        req.setEventSessions(List.of(add));

        UpdateEventPayload payload = new UpdateEventPayload();

        assertThrows(AppException.class,
                () -> service.checkAndResolveUpdateEventDateTime(event, req, payload));
    }

    @Test
    void checkAndResolveUpdateEventDateTime_updateSessions_recruitmentInvalid_shouldThrow() {
                event = mockEventForUpdate();

        OffsetDateTime d = OffsetDateTime.now().plusDays(2);

        EditEventSessionRequest add = req(EUpdateAction.ADD, d, d.plusHours(2));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setEventSessions(List.of(add));

        // recruitmentEndDate hiện tại sẽ vi phạm rule
        event.setRecruitmentEndDate(d.toLocalDate().minusDays(1));

        UpdateEventPayload payload = new UpdateEventPayload();

        assertThrows(AppException.class,
                () -> service.checkAndResolveUpdateEventDateTime(event, req, payload));
    }

    @Test
    void checkAndResolveUpdateEventDateTime_updateBoth_shouldWork() {
        event = mockEventForUpdate();

        OffsetDateTime d = OffsetDateTime.now().plusDays(10);

        EditEventSessionRequest add = req(EUpdateAction.ADD, d, d.plusHours(3));

        UpdateEventRequest req = new UpdateEventRequest();
        req.setEventSessions(List.of(add));
        req.setRecruitmentEndDate(d.toLocalDate().minusDays(5));

        UpdateEventPayload payload = new UpdateEventPayload();

        boolean result = service.checkAndResolveUpdateEventDateTime(event, req, payload);

        assertTrue(result);
        assertEquals(2, payload.getEventSessions().size());
        assertEquals(d.toLocalDate().minusDays(5), payload.getRecruitmentEndDate());
    }
}
