package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.service.impl.EventSessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class EventSessionServiceTest {

    @InjectMocks
    EventSessionServiceImpl service;

    Event event;

    @BeforeEach
    void setup() {
        event = new Event();
        event.setId(UUID.randomUUID());
        event.setImages(new ArrayList<>());
    }

    private OffsetDateTime start(int plusDays) {
        return OffsetDateTime.now().plusDays(plusDays);
    }

    private LocalDate validRecruitmentEndDate() {
        return LocalDate.now().plusDays(10);
    }

    private EditEventSessionRequest sessionReq(OffsetDateTime start, OffsetDateTime end) {
        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(EUpdateAction.ADD);
        r.setStartDateTime(start);
        r.setEndDateTime(end);
        r.setExpectedSerAmount(10);
        r.setExpectedVolAmount(10);
        return r;
    }

    private EditEventSessionRequest removeReq(UUID id) {
        EditEventSessionRequest r = new EditEventSessionRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        r.setEventSessionId(id);
        return r;
    }

    private EditEventSessionRequest editReq(UUID id, OffsetDateTime start, OffsetDateTime end) {
        EditEventSessionRequest r = sessionReq(start, end);
        r.setUpdateAction(EUpdateAction.EDIT);
        r.setEventSessionId(id);
        return r;
    }

    private EventSession session(OffsetDateTime start) {
        EventSession s = new EventSession();
        s.setId(UUID.randomUUID());
        s.setStartDateTime(start);
        s.setEndDateTime(start.plus(Duration.ofHours(2)));
        return s;
    }

    // ==== addEventSessionsForCreateEvent ===================================
    // TC01
    @Test
    void addEventSessions_nullRequest_shouldThrow() {

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(
                        event,
                        validRecruitmentEndDate(),
                        null,
                        (short) 4
                ));
    }

    // TC02
    @Test
    void addEventSessions_emptyRequest_shouldThrow() {

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(
                        event,
                        validRecruitmentEndDate(),
                        List.of(),
                        (short) 4
                ));
    }

    // TC03
    @Test
    void addEventSessions_exceedSessionMaxTime_shouldThrow() {

        EditEventSessionRequest r = sessionReq(
                start(20),
                start(20).plusHours(10) // exceed max
        );

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(
                        event,
                        validRecruitmentEndDate(),
                        List.of(r),
                        (short) 4
                ));
    }

    // TC04
    @Test
    void addEventSessions_oneValid_shouldAdd() {

        EditEventSessionRequest r = sessionReq(
                start(20),
                start(20).plusHours(2)
        );

        service.addEventSessionsForCreateEvent(
                event,
                validRecruitmentEndDate(),
                List.of(r),
                (short) 4
        );

        assertEquals(1, event.getSessions().size());
        assertNotNull(event.getStartDate());
    }

    // TC05
    @Test
    void addEventSessions_multipleValid_shouldAddAll() {

        EditEventSessionRequest r1 = sessionReq(start(20), start(20).plusHours(2));
        EditEventSessionRequest r2 = sessionReq(start(21), start(21).plusHours(2));

        service.addEventSessionsForCreateEvent(
                event,
                validRecruitmentEndDate(),
                List.of(r1, r2),
                (short) 4
        );

        assertEquals(2, event.getSessions().size());
    }

    // TC06
    @Test
    void addEventSessions_duplicateDay_shouldThrow() {

        OffsetDateTime base = OffsetDateTime.of(
                2030, 1, 1, 10, 0, 0, 0,
                ZoneOffset.of("+07:00")
        ).plusDays(20);
        OffsetDateTime start1 = base;
        OffsetDateTime end1 = base.plusHours(1);

        OffsetDateTime start2 = base.plusHours(2);
        OffsetDateTime end2 = base.plusHours(3);

        EditEventSessionRequest r1 = sessionReq(start1, end1);
        EditEventSessionRequest r2 = sessionReq(start2, end2);

        assertThrows(AppException.class,
                () -> service.addEventSessionsForCreateEvent(
                        event,
                        validRecruitmentEndDate(),
                        List.of(r1, r2),
                        (short) 4
                ));
    }

    // ==== updateEventSessions ===================================
    // TC01
    @Test
    void updateEventSessions_nullRequest_shouldReturn() {

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                null,
                (short) 4
        );

        assertTrue(event.getSessions().isEmpty());
    }

    // TC02
    @Test
    void updateEventSessions_emptyRequest_shouldReturn() {

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                List.of(),
                (short) 4
        );

        assertTrue(event.getSessions().isEmpty());
    }

    // TC03
    @Test
    void updateEventSessions_remove_shouldDelete() {

        EventSession s1 = session(start(20));
        EventSession s2 = session(start(21));

        event.getSessions().addAll(List.of(s1, s2));

        EditEventSessionRequest r = removeReq(s1.getId());

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                List.of(r),
                (short) 4
        );

        assertEquals(1, event.getSessions().size());
        assertEquals(s2.getId(), event.getSessions().get(0).getId());
    }

    // TC04
    @Test
    void updateEventSessions_edit_shouldUpdateValues() {

        EventSession s = session(start(20));
        event.getSessions().add(s);

        OffsetDateTime newStart = start(25);
        OffsetDateTime newEnd = newStart.plus(Duration.ofHours(2));

        EditEventSessionRequest r = editReq(
                s.getId(),
                newStart,
                newEnd
        );

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                List.of(r),
                (short) 4
        );

        assertEquals(newStart, s.getStartDateTime());
        assertEquals(newEnd, s.getEndDateTime());
    }

    // TC05
    @Test
    void updateEventSessions_add_shouldInsert() {

        EventSession s = session(start(20));
        event.getSessions().add(s);

        EditEventSessionRequest r = sessionReq(
                start(22),
                start(22).plusHours(2)
        );

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                List.of(r),
                (short) 4
        );

        assertEquals(2, event.getSessions().size());
    }

    // TC06
    @Test
    void updateEventSessions_removeAll_shouldThrow() {

        EventSession s = session(start(20));
        event.getSessions().add(s);

        EditEventSessionRequest r = removeReq(s.getId());

        assertThrows(AppException.class,
                () -> service.updateEventSessions(
                        event,
                        validRecruitmentEndDate(),
                        List.of(r),
                        (short) 4
                ));
    }

    // TC07
    @Test
    void updateEventSessions_duplicateDayAfterEdit_shouldThrow() {

        EventSession s1 = session(start(20));
        EventSession s2 = session(start(21));

        event.getSessions().addAll(List.of(s1, s2));

        EditEventSessionRequest r = editReq(
                s2.getId(),
                start(20),
                start(20).plusHours(2)
        );

        assertThrows(AppException.class,
                () -> service.updateEventSessions(
                        event,
                        validRecruitmentEndDate(),
                        List.of(r),
                        (short) 4
                ));
    }

    // TC08
    @Test
    void updateEventSessions_editNonExistId_shouldIgnoreEdit() {

        EventSession s1 = session(start(20));
        EventSession s2 = session(start(21));

        event.getSessions().addAll(List.of(s1, s2));

        UUID nonExistId = UUID.randomUUID();

        EditEventSessionRequest r = editReq(
                nonExistId,
                start(22),
                start(22).plusHours(2)
        );

        service.updateEventSessions(
                event,
                validRecruitmentEndDate(),
                List.of(r),
                (short) 4
        );

        // sessions remain unchanged
        assertEquals(2, event.getSessions().size());

        EventSession rs1 = event.getSessions().stream()
                .filter(s -> s.getId().equals(s1.getId()))
                .findFirst()
                .orElseThrow();

        EventSession rs2 = event.getSessions().stream()
                .filter(s -> s.getId().equals(s2.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(s1.getStartDateTime(), rs1.getStartDateTime());
        assertEquals(s2.getStartDateTime(), rs2.getStartDateTime());

    }


    // ==== validate ===================================
    private LocalDate invokeValidate(LocalDate recruitmentEndDate, List<EventSession> sessions, Event targetEvent) throws Exception {

        Method m = EventSessionServiceImpl.class
                .getDeclaredMethod(
                        "validateAndResolveEventStartEndDate",
                        LocalDate.class,
                        List.class,
                        Event.class
                );

        m.setAccessible(true);

        return (LocalDate) m.invoke(service, recruitmentEndDate, sessions, targetEvent);
    }

    // TC01
    @Test
    void validateAndResolveStartDate_valid_shouldReturnStartDate() throws Exception {

        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");

        EventSession s1 = session(start(20));
        EventSession s2 = session(start(25));

        LocalDate recruitmentEnd = LocalDate.now(vn).plusDays(10);

        LocalDate result = invokeValidate(
                recruitmentEnd,
                List.of(s1, s2),
                event
        );

        LocalDate expected = s1.getStartDateTime()
                .atZoneSameInstant(vn)
                .toLocalDate();

        assertEquals(event.getStartDate(), s1.getStartDateTime().toLocalDate());
        assertEquals(event.getEndDate(), s1.getEndDateTime().toLocalDate());
    }

    // TC02
    @Test
    void validateAndResolveStartDate_duplicateDay_shouldThrow() {

        OffsetDateTime base = OffsetDateTime.of(
                2030, 1, 1, 10, 0, 0, 0,
                ZoneOffset.of("+07:00")
        ).plusDays(20);

        EventSession s1 = session(base);
        EventSession s2 = session(base.plus(Duration.ofHours(2)));

        LocalDate recruitmentEnd = validRecruitmentEndDate();

        assertThrows(InvocationTargetException.class,
                () -> invokeValidate(recruitmentEnd, List.of(s1, s2), event));
    }

    // TC03
    @Test
    void validateAndResolveStartDate_recruitmentTooSoon_shouldThrow() {

        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");

        EventSession s = session(start(20));

        LocalDate recruitmentEnd = LocalDate.now(vn).plusDays(1);

        assertThrows(InvocationTargetException.class,
                () -> invokeValidate(recruitmentEnd, List.of(s), event));
    }

    // TC04
    @Test
    void validateAndResolveStartDate_startTooSoon_shouldThrow() {

        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");

        EventSession s = session(start(5));

        LocalDate recruitmentEnd = LocalDate.now(vn).plusDays(10);

        assertThrows(InvocationTargetException.class,
                () -> invokeValidate(recruitmentEnd, List.of(s), event));
    }

    // TC05
    @Test
    void validateAndResolveStartDate_recruitmentTooLate_shouldThrow() {

        ZoneId vn = ZoneId.of("Asia/Ho_Chi_Minh");

        OffsetDateTime start = start(20);

        EventSession s = session(start);

        LocalDate startDate = start.atZoneSameInstant(vn).toLocalDate();

        LocalDate recruitmentEnd = startDate.minusDays(2);

        assertThrows(InvocationTargetException.class,
                () -> invokeValidate(recruitmentEnd, List.of(s), event));
    }
}
