package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.EventSessionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventSessionServiceImpl implements EventSessionService {
    private final EventSessionRepository eventSessionRepository;

    /*
    In context of this class 1 session equivalence to 1 EventDateTime
    I use the word "session" in some place for short
     */

    @Override
    @Transactional
    public void addEventSessionsForCreateEvent(
            Event event,
            LocalDate recruitmentEndDate,
            List<EditEventSessionRequest> sessionRequests,
            Short sessionMaxTime
    ){
        if (sessionRequests == null || sessionRequests.isEmpty()) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }

        //check request: valid add place amount?
        List<EditEventSessionRequest> adds = sessionRequests.stream()
                .filter(r -> (
                        r.getUpdateAction() == EUpdateAction.ADD
                        && Duration.between(r.getStartDateTime(), r.getEndDateTime()).compareTo(Duration.ofHours(sessionMaxTime)) <= 0)
                )
                .toList();

        //make sure at least 1 object is added
        if (adds.isEmpty()) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }
        addEventDateTimes(event, adds);

        LocalDate startDate = validateAndResolveStartDate(recruitmentEndDate, event.getDateTimes());

        event.setStartDate(startDate);
    }

    private void addEventDateTimes(
            Event event,
            List<EditEventSessionRequest> addRequestList
    ) {
        for (EditEventSessionRequest r : addRequestList) {
            EventSession dt = new EventSession();
            dt.setEvent(event);
            dt.setStartDateTime(r.getStartDateTime());
            dt.setEndDateTime(r.getEndDateTime());
            dt.setExpectedVolAmount(r.getExpectedVolAmount());
            dt.setExpectedSerAmount(r.getExpectedSerAmount());

            event.getDateTimes().add(dt);
        }
    }

    @Override
    @Transactional
    public void updateEventSessions(
            Event event,
            LocalDate recruitmentEndDate,
            List<EditEventSessionRequest> sessionRequests,
            Short sessionMaxTime
    ) {
        //request datetime empty, don't need to update
        if (sessionRequests == null || sessionRequests.isEmpty()) return;

        List<EventSession> existingEventSessions = event.getDateTimes();

        //categorize update place request base on action
        List<EditEventSessionRequest> removes = new ArrayList<>();
        List<EditEventSessionRequest> edits = new ArrayList<>();
        List<EditEventSessionRequest> adds = new ArrayList<>();
        for (EditEventSessionRequest r : sessionRequests) {
            if (r.getUpdateAction() == EUpdateAction.REMOVE) {
                removes.add(r);
                //done remove, skip code below
                continue;
            }

            if (
                    Duration.between(r.getStartDateTime(), r.getEndDateTime())
                            .compareTo(Duration.ofHours(sessionMaxTime)) > 0
            ) {
                //not satisfy session constraint
                throw new AppException(EventErrorCode.INVALID_EVENT_SESSION_TIME_RANGE);
            }

            if (r.getUpdateAction() == EUpdateAction.EDIT) {
                edits.add(r);
            } else if (r.getUpdateAction() == EUpdateAction.ADD) {
                adds.add(r);
            }
        }

        //check the amount
        Set<UUID> existingIds = existingEventSessions.stream()
                .map(EventSession::getId)
                .collect(Collectors.toSet());

        Set<UUID> removeIds = removes.stream()
                .map(EditEventSessionRequest::getEventSessionId)
                .filter(Objects::nonNull)
                .filter(existingIds::contains)
                .collect(Collectors.toSet());

        int finalCount = existingEventSessions.size() - removeIds.size() + adds.size();

        if (finalCount < 1) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }

        //REMOVE
        if (!removeIds.isEmpty()) {

            existingEventSessions.removeIf(dt -> removeIds.contains(dt.getId()));
        }

        //EDIT
        if (!edits.isEmpty()) {
            //transfer to map for the edit step
            Map<UUID, EditEventSessionRequest> editMap = edits.stream()
                    .filter(r -> r.getEventSessionId() != null)
                    .filter(r -> existingIds.contains(r.getEventSessionId()))
                    .collect(Collectors.toMap(
                            EditEventSessionRequest::getEventSessionId,
                            r -> r,
                            (a, b) -> b   // request same id, keep the last one
                    ));

            //edit step
            //iterate through the list in event and update the one with same id
            for (EventSession dt : existingEventSessions) {

                EditEventSessionRequest r = editMap.get(dt.getId());
                if (r == null) continue;

                dt.setStartDateTime(r.getStartDateTime());
                dt.setEndDateTime(r.getEndDateTime());
                dt.setExpectedVolAmount(r.getExpectedVolAmount());
                dt.setExpectedSerAmount(r.getExpectedSerAmount());
            }
        }

        //ADD
        if (!adds.isEmpty()) {
            addEventDateTimes(event, adds);
        }

        // VALIDATE + RESOLVE START DATE
        LocalDate startDate = validateAndResolveStartDate(
                recruitmentEndDate,
                existingEventSessions
        );
        event.setStartDate(startDate);

    }

    private LocalDate validateAndResolveStartDate(
            LocalDate recruitmentEndDate,
            List<EventSession> sessions
    ) {

        Set<LocalDate> days = new HashSet<>();
        //iterate through each session to make sure there are no 2 session in one day
        for (EventSession r : sessions) {
            // convert UTC -> VN
            LocalDate date = r.getStartDateTime()
                    .toLocalDate();

            // there is no 2 session in a same day
            if (!days.add(date)) {
                throw new AppException(EventErrorCode.DUPLICATE_SESSION_DAY);
            }
        }

        LocalDate startDate = days.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();

        // today
        LocalDate today = LocalDate.now();

        // startDate endDate must after at least 15 days since today
        if (startDate.isBefore(today.plusDays(15))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_START_DATE);
        }

        //recruitment endDate must after at least 3 days since today
        if (recruitmentEndDate.isBefore(today.plusDays(3))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_RECRUITMENT_END_DATE);
        }

        // recruitmentEndDate must before startDate at least 3 days
        if (!recruitmentEndDate.isBefore(startDate.minusDays(3))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_RECRUITMENT_END_DATE);
        }

        return startDate;
    }

    @Override
    public List<EventSession> findConflictSessionDateOfHost(UUID hostId, UUID checkedEventId, List<EventSession> checkedSessions) {
        List<LocalDate> dates = checkedSessions
                .stream()
                .map(s -> s.getStartDateTime().toLocalDate())
                .toList();

        return eventSessionRepository.findConflictingSessions(
                hostId,
                checkedEventId,
                dates
        );
    }
}
