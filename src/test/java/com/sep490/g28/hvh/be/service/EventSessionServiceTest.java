package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.impl.EventSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventSessionServiceTest {

    @Mock
    private EventSessionRepository eventSessionRepository;

    @InjectMocks
    private EventSessionServiceImpl service;

    private Event event;

    @BeforeEach
    void init() {
        event = event(); // gọi factory method
    }

    Event event() {
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
        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, null));
    }

    // TC02
    @Test
    void emptyRequest_shouldThrow() {
        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of()));
    }

    // TC03
    @Test
    void noAddAction_shouldThrow() {
        EditEventSessionRequest r = req(EUpdateAction.EDIT);

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(event, List.of(r)));
    }

    // TC04
    @Test
    void durationTooLong_shouldThrow() {
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



}
