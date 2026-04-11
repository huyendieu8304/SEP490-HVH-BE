package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventSessionPayload;
import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.dto.eventsession.projection.SessionEventProjection;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.entity.ActivityDomain;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.EventSessionService;
import com.sep490.g28.hvh.be.service.NotificationService;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventSessionServiceImpl implements EventSessionService {
   EventSessionRepository eventSessionRepository;
   EventApplicationRepository eventApplicationRepository;

   NotificationService notificationService;

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
            session.setApprovedApplicationCount(0);
            session.setCheckInCode(RandomStringUtil.random6Numberic());
            return session;
        }).toList());

        // VALIDATE + RESOLVE START DATE END DATE
        Set<LocalDate> sessionDates = getSessionDates(event.getSessions());

        if (!findConflictSessionDateOfHost(
                event.getHost().getId(),
                event.getId(),
                event.getSessions()
        ).isEmpty()) {
            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
        }
        //resolve event's startDate
        LocalDate startDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //resolve event's endDate
        LocalDate endDate = sessionDates.stream()
                .max(LocalDate::compareTo)
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

        if (!findConflictSessionDateOfHost(
                event.getHost().getId(),
                event.getId(),
                event.getSessions()
        ).isEmpty()) {
            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
        }

        //resolve event's startDate
        LocalDate startDate = sessionDates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();
        //resolve event's endDate
        LocalDate endDate = sessionDates.stream()
                .max(LocalDate::compareTo)
                .orElseThrow();
        //check event's dates constraints
        checkEventDatesConstraint(startDate, event.getRecruitmentEndDate());

        event.setStartDate(startDate);
        event.setEndDate(endDate);
    }

    //todo unit test for this method
    @Override
    public List<EventSession> findConflictSessionDateOfHost(UUID hostId, UUID checkedEventId, List<EventSession> checkedSessions) {
        if (checkedSessions == null || checkedSessions.isEmpty()) {
            return Collections.emptyList();
        }

        // get all session of host, not in this event, those events are at least approved by org mng
        List<EventSession> allSessions = eventSessionRepository.findByHostExcludingEvent(
                hostId,
                checkedEventId,
                Arrays.asList(
                        EEventStatus.APPROVED_BY_MNG.name(),
                        EEventStatus.RECRUITING.name(),
                        EEventStatus.UPCOMING.name(),
                        EEventStatus.ONGOING.name()
                )
        );

        Set<LocalDate> checkedDatesVN = checkedSessions.stream()
                .map(s -> s.getStartDateTime().toLocalDate()) // VN time zone
                .collect(Collectors.toSet());

        return allSessions.stream()
                .filter(s -> checkedDatesVN.contains(s.getStartDateTime().toLocalDate()))
                .toList();

    }

    private void checkEventDatesConstraint(LocalDate startDate, LocalDate newRecruitmentEndDate) {
        // today
        LocalDate today = LocalDate.now();

        // startDate must after at least 15 days since today (sd >= today +15
        if (startDate.isBefore(today.plusDays(15))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_START_DATE);
        }

        // recruitmentEndDate must before startDate at least 3 days (red <= sd -3)
        if (newRecruitmentEndDate.isAfter(startDate.minusDays(3))) {
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
            for (EventSession session : targetEventSessionsList) {

                EditEventSessionRequest r = editMap.get(session.getId());
                if (r == null) continue;

                session.setStartDateTime(r.getStartDateTime());
                session.setEndDateTime(r.getEndDateTime());
                session.setExpectedVolAmount(r.getExpectedVolAmount());
                session.setExpectedSerAmount(r.getExpectedSerAmount());
                session.setApprovedApplicationCount(0);
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
                        session.setApprovedApplicationCount(0);
                        session.setCheckInCode(RandomStringUtil.random6Numberic());
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
                && !updateEventRequest.getRecruitmentEndDate().isEqual(event.getRecruitmentEndDate())
        ) {
            updateEventDateTime = true;
            recruitmentEndDateAfterUpdate = updateEventRequest.getRecruitmentEndDate();
            updateEventPayload.setRecruitmentEndDate(recruitmentEndDateAfterUpdate);
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
            List<EventSession> conflictSession = findConflictSessionDateOfHost(
                    event.getHost().getId(),
                    event.getId(),
                    eventSessionsAfterUpdate
            );

            if (conflictSession != null && !conflictSession.isEmpty()) {
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

            List<UpdateEventSessionPayload> sessionPayloads = eventSessionsAfterUpdate.stream().map(s -> {
                UpdateEventSessionPayload p = new UpdateEventSessionPayload();
                p.setId(s.getId());
                p.setStartDateTime(s.getStartDateTime());
                p.setEndDateTime(s.getEndDateTime());
                p.setExpectedVolAmount(s.getExpectedVolAmount());
                p.setExpectedSerAmount(s.getExpectedSerAmount());
                return p;
            }).toList();

            updateEventPayload.setEventSessions(sessionPayloads);
        }

        // event recruitment end date OR event session is updated
        // recruitmentEndDate must before startDate at least 3 days  (red <= sd-3)
        if (updateEventDateTime && recruitmentEndDateAfterUpdate.isAfter(startDateAfterUpdate.minusDays(3))) {
            throw new AppException(EventErrorCode.INVALID_EVENT_RECRUITMENT_END_DATE);
        }

        return updateEventDateTime;

    }

    @Override
    public List<EventSession> resolveUpdateEventSessions(
            Event event,
            List<EventSession> oldSessions,
            List<UpdateEventSessionPayload> payloads) {

        // Map existing
        Map<UUID, EventSession> existing = oldSessions.stream()
                .filter(s -> s.getId() != null)
                .collect(Collectors.toMap(EventSession::getId, Function.identity()));

        List<EventSession> result = new ArrayList<>(payloads.size());
        Set<UUID> keepIds = new HashSet<>();

        // Build new list
        for (UpdateEventSessionPayload p : payloads) {
            UUID id = p.getId();

            if (id != null && existing.containsKey(id)) {
                // update
                EventSession s = existing.get(id);
                s.setStartDateTime(p.getStartDateTime());
                s.setEndDateTime(p.getEndDateTime());
                s.setExpectedVolAmount(p.getExpectedVolAmount());
                s.setExpectedSerAmount(p.getExpectedSerAmount());
                s.setApprovedApplicationCount(0); //reset the number of application approved

                result.add(s);
                keepIds.add(id);
            } else {
                // create
                EventSession s = new EventSession();
                s.setEvent(event);
                s.setStartDateTime(p.getStartDateTime());
                s.setEndDateTime(p.getEndDateTime());
                s.setExpectedVolAmount(p.getExpectedVolAmount());
                s.setExpectedSerAmount(p.getExpectedSerAmount());
                s.setApprovedApplicationCount(0); //reset the number of application approved
                s.setCheckInCode(RandomStringUtil.random6Numberic());
                result.add(s);
            }
        }

        // Remove orphan
        // MUST HAVE orphanRemoval = true IN ENTITY, yeah, I already has it
        List<EventSession> toRemove = oldSessions.stream()
                .filter(s -> s.getId() != null)
                .filter(s -> !keepIds.contains(s.getId()))
                .toList();

        oldSessions.removeAll(toRemove);

        //clear old session and replace by new session
        oldSessions.clear();
        oldSessions.addAll(result);
        return oldSessions;
    }

    @Override
    @Transactional
    public void createCheckInCode() {
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

        //clear old check in code
        LocalDate yesterday = LocalDate.now().minusDays(1);
        OffsetDateTime endOfYesterday = yesterday.atTime(LocalTime.MAX)
                .atZone(vnZone)
                .toOffsetDateTime();

        eventSessionRepository.clearOldCheckInCode(endOfYesterday);
        log.info("Cleared old check in codes");

        //get event session happen today and the event is ONGOING
        LocalDate today = LocalDate.now();


        OffsetDateTime start = today.atStartOfDay(vnZone).toOffsetDateTime();
        OffsetDateTime end = today.atTime(LocalTime.MAX)
                .atZone(vnZone)
                .toOffsetDateTime();

        //find event session that will be hosted to day
        List<SessionEventProjection> projections = eventSessionRepository
                .findSessionHostedOfOngoingEventBetweenIncluded(start, end);

        Set<String> checkInCodes = new HashSet<>();
        List<EventSession> updateSession = new ArrayList<>();
        String checkInCode;
        //iterate through each session to set check in code
        for (SessionEventProjection p : projections) {
            EventSession session = p.getSession();
            //this loop can only make sure it unique in this batch
            do {
             checkInCode = RandomStringUtil.random6Numberic();
            } while (!checkInCodes.add(checkInCode));

            session.setCheckInCode(checkInCode);
            updateSession.add(session);
        }
        eventSessionRepository.saveAll(updateSession);

        //iterate through each session to send notifications to vols and host
        for (SessionEventProjection p : projections) {
            EventSession session = p.getSession();
            List<EventApplication> applications =
                    eventApplicationRepository.findApprovedApplicationBySessionId(session.getId());

            //send notification about the check in code for volunteer
            notificationService.sendCheckInCodeOfEventSessionNotifications(applications, p.getEventName(), session.getCheckInCode());

            //send notification about the event for host
            notificationService.sendEventSessionHostedTodayNotification(p.getHostId(), p.getEventId(), p.getEventName());
        }

        log.info("Created check in codes and send notification to vol and host of today's event session");
    }
}
