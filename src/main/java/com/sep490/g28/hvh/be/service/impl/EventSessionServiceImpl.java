package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.ActivityDomain;
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
            List<EditEventSessionRequest> sessionRequests
    ){
        if (sessionRequests == null || sessionRequests.isEmpty()) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }
        ActivityDomain activityDomain = event.getActivitySubDomain().getActivityDomain();
        Short sessionMaxTime =
                activityDomain.getSpecialSessionMaxTime() == null
                        ? 4
                        : activityDomain.getSpecialSessionMaxTime();
        List<EditEventSessionRequest> adds = new ArrayList<>();
        //check the duration between the start time and end time of the session, must not > session max time of the domain
        for (EditEventSessionRequest r : sessionRequests) {
            if (r.getUpdateAction().equals(EUpdateAction.ADD)){
                if (
                        Duration.between(r.getStartDateTime(), r.getEndDateTime())
                                .compareTo(Duration.ofHours(sessionMaxTime)) > 0
                ) {
                    //not satisfy session constraint
                    throw new AppException(EventErrorCode.INVALID_EVENT_SESSION_TIME_RANGE);
                } else {
                    adds.add(r);
                }
            }
        }

        //check request: valid session time amount?
        //make sure at least 1 object is added
        if (adds.isEmpty()) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }

        event.setSessions(adds.stream().map( r -> {
            EventSession session = new EventSession();
            session.setEvent(event);
            session.setStartDateTime(r.getStartDateTime());
            session.setEndDateTime(r.getEndDateTime());
            session.setExpectedVolAmount(r.getExpectedVolAmount());
            session.setExpectedSerAmount(r.getExpectedSerAmount());
            return session;
        }).toList());

        // VALIDATE + RESOLVE START DATE END DATE
        Set<LocalDate> sessionDates = getSessionDates(event.getSessions());

        if (findConflictSessionDateOfHost(
                event.getHost().getId(),
                event.getId(),
                event.getSessions()
        ) != null) {
            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
        }
        //resolve event's startDate
        LocalDate startDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //resolve event's endDate
        LocalDate endDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //check event's dates constraints
        checkEventDatesConstraint(startDate, event.getRecruitmentEndDate());

        event.setStartDate(startDate);
        event.setEndDate(endDate);
    }

    @Override
    public void updateEventSessions(
            Event event,
            List<EditEventSessionRequest> sessionRequests
    ) {
        //request datetime empty, don't need to update
        if (sessionRequests == null || sessionRequests.isEmpty()) return;

        updateEventSessions(event, sessionRequests, event.getSessions());

        // VALIDATE + RESOLVE START DATE END DATE
        Set<LocalDate> sessionDates = getSessionDates(event.getSessions());

        if (findConflictSessionDateOfHost(
                event.getHost().getId(),
                event.getId(),
                event.getSessions()
        ) != null) {
            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
        }

        //resolve event's startDate
        LocalDate startDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //resolve event's endDate
        LocalDate endDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //check event's dates constraints
        checkEventDatesConstraint(startDate, event.getRecruitmentEndDate());

        event.setStartDate(startDate);
        event.setEndDate(endDate);
    }

    //todo unit test for this method
    //todo check luôn từ lúc tạo sự kiên, update sự kiện cũng check
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

    private void checkEventDatesConstraint(LocalDate startDate, LocalDate newRecruitmentEndDate) {
        // today
        LocalDate today = LocalDate.now();

        // startDate must after at least 15 days since today
        if (startDate.isBefore(today.plusDays(15))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_START_DATE);
        }

        //recruitmentEndDate must after at least 3 days since today
        if (newRecruitmentEndDate.isBefore(today.plusDays(3))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_RECRUITMENT_END_DATE);
        }

        // recruitmentEndDate must before startDate at least 3 days
        if (!newRecruitmentEndDate.isBefore(startDate.minusDays(3))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_RECRUITMENT_END_DATE);
        }
    }

    private Set<LocalDate> getSessionDates(List<EventSession> newEventSessions) {
        Set<LocalDate> days = new HashSet<>();
        //iterate through each session to make sure there are no 2 session in one day
        for (EventSession r : newEventSessions) {
            LocalDate date = r.getStartDateTime()
                    .toLocalDate();

            // there is 2 sessions in a same day
            if (!days.add(date)) {
                throw new AppException(EventErrorCode.DUPLICATE_SESSION_DAY);
            }
        }
        return days;
    }

    private void updateEventSessions(
            Event event,
            List<EditEventSessionRequest> editEventSessionRequests,
            List<EventSession> targetEventSessionsList
    ) {
        ActivityDomain activityDomain = event.getActivitySubDomain().getActivityDomain();
        Short sessionMaxTime =
                activityDomain.getSpecialSessionMaxTime() == null
                        ? 4
                        : activityDomain.getSpecialSessionMaxTime();

        //categorize update place request base on action
        List<EditEventSessionRequest> removes = new ArrayList<>();
        List<EditEventSessionRequest> edits = new ArrayList<>();
        List<EditEventSessionRequest> adds = new ArrayList<>();

        for (EditEventSessionRequest r :  editEventSessionRequests) {
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
        Set<UUID> existingIds = targetEventSessionsList.stream()
                .map(EventSession::getId)
                .collect(Collectors.toSet());

        Set<UUID> removeIds = removes.stream()
                .map(EditEventSessionRequest::getEventSessionId)
                .filter(Objects::nonNull)
                .filter(existingIds::contains)
                .collect(Collectors.toSet());

        int finalCount = targetEventSessionsList.size() - removeIds.size() + adds.size();

        if (finalCount < 1) {
            throw new AppException(EventErrorCode.INVALID_DATE_TIME_AMOUNT);
        }

        //REMOVE
        if (!removeIds.isEmpty()) {
            targetEventSessionsList.removeIf(dt -> removeIds.contains(dt.getId()));
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
            //iterate through the existing event session and update the one with same id
            for (EventSession dt : targetEventSessionsList) {

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
            targetEventSessionsList.addAll(
                    adds.stream().map(r -> {
                        EventSession session = new EventSession();
                        session.setEvent(event);
                        session.setStartDateTime(r.getStartDateTime());
                        session.setEndDateTime(r.getEndDateTime());
                        session.setExpectedVolAmount(r.getExpectedVolAmount());
                        session.setExpectedSerAmount(r.getExpectedSerAmount());
                        return session;
                    }).toList()
            );
        }
    }


    //return true  if valid update
    //false if no update has been made
    @Override
    public boolean checkAndResolveUpdateEventDateTime(
            Event event,
            UpdateEventRequest updateEventRequest,
            UpdateEventPayload  updateEventPayload
    ){
        boolean updateEventDateTime = false;
        LocalDate recruitmentEndDateAfterUpdate = event.getRecruitmentEndDate();
        LocalDate startDateAfterUpdate = event.getStartDate();

        //recruitmentEndDate is updated?
        if (updateEventRequest.getRecruitmentEndDate() != null
                && updateEventRequest.getRecruitmentEndDate().isEqual(event.getRecruitmentEndDate())
        ) {
            updateEventDateTime = true;
            recruitmentEndDateAfterUpdate = updateEventRequest.getRecruitmentEndDate();
            updateEventPayload.setRecruitmentEndDate(updateEventRequest.getRecruitmentEndDate());
        }

        //event sessions is updated?
        if (updateEventRequest.getEventSessions() != null
                && !updateEventRequest.getEventSessions().isEmpty()
        ) {
            updateEventDateTime = true;

            //clone the existing session to new list
            List<EventSession> eventSessionsAfterUpdate = new ArrayList<>(event.getSessions().stream()
                    .map(EventSession::new)
                    .toList());

            //resolve new event sessions
            updateEventSessions(event, updateEventRequest.getEventSessions(), eventSessionsAfterUpdate);

            //check duplicate session date hosted by host after updated
            if (findConflictSessionDateOfHost(
                    event.getHost().getId(),
                    event.getId(),
                    eventSessionsAfterUpdate
            ) != null) {
                throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
            }

            //get all the date of event session and check whether multiple sessions in a same date?
            Set<LocalDate> sessionDates = getSessionDates(eventSessionsAfterUpdate);

            //resolve event's startDate
            startDateAfterUpdate = sessionDates.stream()
                    .min(LocalDate::compareTo)
                    .orElseThrow();

            //resolve event's endDate
            LocalDate endDateAfterUpdate = sessionDates.stream()
                    .min(LocalDate::compareTo)
                    .orElseThrow();
            updateEventPayload.setStartDate(startDateAfterUpdate);
            updateEventPayload.setEndDate(endDateAfterUpdate);
            updateEventPayload.setEventSessions(eventSessionsAfterUpdate);
        }

        // event recruitment end date OR event session is updated
        if (updateEventDateTime) {
            //check event's dates constraints
            checkEventDatesConstraint(startDateAfterUpdate, recruitmentEndDateAfterUpdate);
        }

        return updateEventDateTime;

    }
}
