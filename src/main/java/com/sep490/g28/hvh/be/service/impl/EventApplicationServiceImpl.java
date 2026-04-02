package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckOutEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.EventApplicationService;
import com.sep490.g28.hvh.be.service.NotificationService;
import com.sep490.g28.hvh.be.util.GeoUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventApplicationServiceImpl implements EventApplicationService {
    EventSessionRepository eventSessionRepository;
    EventApplicationRepository eventApplicationRepository;
    VolunteerRepository volunteerRepository;
    EventRepository eventRepository;
    CheckInLogRepository checkInLogRepository;
    StorageService storageService;

    CurrentUserProvider currentUserProvider;

    NotificationService notificationService;

    @Transactional
    @Override
    public void applyEventSession(UUID sessionId) {
        //find the session
        EventSession session = eventSessionRepository.findById(sessionId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED));

        Event event = session.getEvent();
        //only allow application when the event status is RECRUITING
        if (!event.getStatus().equals(EEventStatus.RECRUITING)) {
            throw new AppException(EventErrorCode.EVENT_NOT_RECRUITING);
        }

        //todo check again after finish all event status, might not need to check the bellow
        //check registration deadline
        if (LocalDate.now().isAfter(event.getRecruitmentEndDate())) {
            throw new AppException(EventErrorCode.EVENT_RECRUITMENT_CLOSED);
        }

        UUID volunteerId = currentUserProvider.getId();

        //Have ever the volunteer applied for this session yet?
        if (eventApplicationRepository.findApplicationPendingOrApproved(volunteerId, sessionId).isPresent()) {
            //rejected and cancelled still can apply again
            //but pending and approve -> nah
            throw new AppException(EventErrorCode.ALREADY_APPLIED);
        }
        //check expected Vol amount
        if (session.getExpectedVolAmount() == session.getApprovedApplicationCount()) {
            throw new AppException(EventErrorCode.EVENT_SESSION_FULL);
        }

        //check the session overlap with any applied session
        if (eventApplicationRepository.findOverlapSession(
                volunteerId,
                session.getStartDateTime().toLocalDate(),
                session.getStartDateTime(),
                session.getEndDateTime()
        ) != null
        ) {
            throw new AppException(EventErrorCode.APPLYING_SESSION_TIME_CONFLICT);
        }

        EventApplication eventApplication = new EventApplication();
        eventApplication.setSession(session);
        eventApplication.setVolunteer(volunteerRepository.getReferenceById(volunteerId));
        eventApplication.setSessionDate(session.getStartDateTime().toLocalDate());

        //check auto approve
        if (event.isAutoApprove()) {
            eventApplication.setStatus(EEventApplicationStatus.APPROVED);
            session.setApprovedApplicationCount(session.getApprovedApplicationCount() + 1);

            eventApplication = eventApplicationRepository.save(eventApplication);
            eventSessionRepository.save(session);

            //subscribe the volunteer's notification token(s) to the topic of notification
            notificationService.subscribeUserToTopicOfEvent(volunteerId, event.getId());
            log.info("Volunteer application is approved automatically eventApplicationId={}", eventApplication.getId());

        } else {
            eventApplication.setStatus(EEventApplicationStatus.PENDING);
            eventApplication = eventApplicationRepository.save(eventApplication);
            log.info("Volunteer application is created with PENDING status eventApplicationId={}", eventApplication.getId());
        }
    }

    @Transactional
    @Override
    public void approveApplication(UUID applicationId) {
        //find the application
        EventApplication eventApplication = eventApplicationRepository.findById(applicationId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED)
        );

        //check the status of the application
        if (!eventApplication.getStatus().equals(EEventApplicationStatus.PENDING)) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_PENDING);
        }
        //check the expected amount
        EventSession eventSession = eventApplication.getSession();
        if (eventSession.getApprovedApplicationCount() >= eventSession.getExpectedVolAmount()) {
            throw new AppException(EventErrorCode.EVENT_SESSION_FULL);
        }

        //check the status of the event
        Event event = eventSession.getEvent();
        if (!EEventStatus.canEventApplicationBeProcessedByHost(event.getStatus())){
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_PROCESS);
        }

        eventApplication.setStatus(EEventApplicationStatus.APPROVED);
        eventApplicationRepository.save(eventApplication);

        eventSession.setApprovedApplicationCount(eventSession.getApprovedApplicationCount() + 1);
        eventSessionRepository.save(eventSession);

        //subscribe the volunteer's notification token(s) to the topic of notification
        notificationService.subscribeUserToTopicOfEvent(eventApplication.getVolunteer().getId(), event.getId());

        //send notification to vol
        notificationService.sendEventApplicationApprovedNotification(eventApplication.getVolunteer().getId(), event, eventApplication);
        log.info("Approved event application eventApplicationId={}", eventApplication.getId());
    }

    @Override
    public void rejectApplication(UUID applicationId, RejectApplicationRequest request) {
        //find the application
        EventApplication eventApplication = eventApplicationRepository.findById(applicationId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED)
        );

        //check the status of the application
        if (!eventApplication.getStatus().equals(EEventApplicationStatus.PENDING)) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_PENDING);
        }

        //todo liệu có cần kiểm tra thông tin status của event ở chỗ này không?
        //todo có khi thêm cron job, khi event chuyển status qua ONGOING cái là tự động reject hết đơn đăng kí luôn
        eventApplication.setStatus(EEventApplicationStatus.REJECTED);
        eventApplicationRepository.save(eventApplication);
        log.info("Reject event application eventApplicationId={}", eventApplication.getId());

        Event event = eventApplication.getSession().getEvent();

        //send notification to vol
        notificationService.sendEventApplicationRejectedNotification(
                eventApplication.getVolunteer().getId(),
                event,
                eventApplication,
                request.getRejectionReason()
        );
    }

    @Transactional
    @Override
    public void cancelApplication(UUID applicationId) {
        //find the application
        EventApplication eventApplication = eventApplicationRepository.findById(applicationId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED)
        );

        EventSession eventSession = eventApplication.getSession();
        Event event = eventSession.getEvent();
        Volunteer volunteer = eventApplication.getVolunteer();

        //whether the event status allow volunteer to cancel application
        if (!EEventStatus.canEventApplicationBeCancelledByVolunteer(event.getStatus())){
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCELLED);
        }

        //check application status, only PENDING and APPROVED can cancel
        if (eventApplication.getStatus().equals(EEventApplicationStatus.CANCELLED)
                || eventApplication.getStatus().equals(EEventApplicationStatus.REJECTED)) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCELLED);
        }
        LocalDate today = LocalDate.now();

        // not allow to cancel on the date or after the session date
        if (!today.isBefore(eventApplication.getSessionDate())) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCELLED);
        }

        boolean isMinusScore = false;
        //application is approved -> check the event timeline
        if (eventApplication.getStatus().equals(EEventApplicationStatus.APPROVED)) {
            //check event status
            /*
            If an application is approved
            and the volunteer cancels the applied event
            after the recruitment end date
            and before the  date of the event session,
            volunteer's honor score will be minus for 3 scores.
             */
            if (today.isAfter(event.getRecruitmentEndDate())) {
                // volunteer's honor score will be minus for 3 scores
                volunteer.setHonorScore((short) (volunteer.getHonorScore() - 3));
                volunteerRepository.save(volunteer);
                log.info("Volunteer will be deduct 3 points of honor score after cancel application successfully");
                isMinusScore = true;
            }

            //decrease the approved amount of session
            eventSession.setApprovedApplicationCount(eventSession.getApprovedApplicationCount() - 1);
            eventSessionRepository.save(eventSession);
        }

        eventApplication.setStatus(EEventApplicationStatus.CANCELLED);
        eventApplicationRepository.save(eventApplication);

        //unsubscribe the volunteer's notification token(s) from the topic of notification
        notificationService.unsubscribeUserFromTopicOfEvent(currentUserProvider.getId(), event.getId());

        //send notification to the volunteer
        notificationService.sendEventApplicationCancelledSuccessfullyNotification(volunteer.getId(), event, eventApplication, isMinusScore);
        log.info("Volunteer cancelled event application eventApplicationId={}", eventApplication.getId());
    }

    @Override
    public EventApplicationsResponse getRegisteredParticipants(int pageNumber, int pageSize, UUID sessionId) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventApplication> page = eventApplicationRepository.getEventApplicationsBySessionId(sessionId, pageable);

        List<RegisteredParticipantSimpleResponse> responses = Optional.of(page.getContent())
                .map(list -> list.stream()
                        .filter(e -> e.getStatus().equals(EEventApplicationStatus.PENDING))
                        .map(e -> {

                            UUID volunteerId = null;
                            String email = null;
                            String phone = null;
                            String nickName = null;
                            String name = null;
                            String avatarUrl = null;
                            String address = null;
                            Short creditScore = 0;
                            Short honorScore = 0;
                            OffsetDateTime createdAt = null;

                            //check if the event application linked with a volunteer
                            if (e.getVolunteer() != null) {

                                Volunteer volunteer = e.getVolunteer();

                                volunteerId = volunteer.getId();
                                email = volunteer.getEmail();
                                phone = volunteer.getPhone();
                                nickName = volunteer.getNickname();
                                name = volunteer.getFullName();
                                address = volunteer.getAddress();
                                creditScore = volunteer.getCreditScore();
                                honorScore = volunteer.getHonorScore();
                                createdAt = volunteer.getCreatedAt();


                                //get signed URL of file
                                if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {

                                    CompletableFuture<String> avatarFuture =
                                            storageService.getSignedUrlAsync(volunteer.getAvatarUrl());

                                    try {
                                        CompletableFuture.allOf(avatarFuture).join();
                                        avatarUrl = avatarFuture.join();
                                    } catch (CompletionException ex) {
                                        Throwable cause = ex.getCause();
                                        if (cause instanceof AppException ae) {
                                            //todo: handle app exception in viewEventFeeds
                                        } else {
                                            throw cause instanceof RuntimeException re ? re : ex;
                                        }
                                    }
                                }
                            }

                            return new RegisteredParticipantSimpleResponse(
                                    volunteerId,
                                    email,
                                    phone,
                                    nickName,
                                    name,
                                    avatarUrl,
                                    address,
                                    creditScore,
                                    honorScore,
                                    createdAt
                            );
                        }).toList()).orElse(Collections.emptyList());

        // If after load the page with n size,
        // and page.hasNext() is true (the slice will auto check this)
        // , move the cursor to the next page, which will load more content
        // (equivalent to call the api one more time)
        return new EventApplicationsResponse(
                responses,
                page.hasNext() ? String.valueOf(pageNumber + 1) : null,
                page.hasNext()
        );
    }

    @Override
    public List<EventApplication> cancelAllApplicationsOfEvent(Event event) {
        //update all the applications of the volunteer to CANCELLED status
        List<EventSession> eventSessions = event.getSessions();
        List<UUID> sessionIds = eventSessions.stream().map(EventSession::getId).toList();
        List<EventApplication> eventApplications =  eventApplicationRepository.cancelApplicationsBySessions(sessionIds);
        log.info("All the applications of volunteer has been cancelled");
        return eventApplications;
    }

    @Override
    public Page<EventApplicationsStatusResponse> getEventApplicationsStatus(int pageNumber, int pageSize, String inputStatus) {
        UUID volunteerId = currentUserProvider.getId();

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        EEventApplicationStatus status =
                (inputStatus == null || inputStatus.isBlank())
                        ? null
                        : EEventApplicationStatus.valueOf(inputStatus);

        Page<EventApplication> eventApplication = eventApplicationRepository.findByVolunteerId(volunteerId, status, pageable);

        if(eventApplication.getContent().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, eventApplication.getTotalElements());
        }

        return eventApplication.map(e -> {

            Event event = e.getSession().getEvent();

            String firstEventImageUrl = null;

            //get signed URL of file
            if (event.getImages() != null && !event.getImages().isEmpty()) {

                log.info("image of event: " + event.getImages());

                List<EventImage> eventImageList = event.getImages();

                CompletableFuture<String> firstEventImageFuture =
                        storageService.getSignedUrlAsync(eventImageList.getFirst().getImagePath());

                try {
                    CompletableFuture.allOf(firstEventImageFuture).join();
                    firstEventImageUrl = firstEventImageFuture.join();
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AppException ae) {
                        //todo: handle app exception in getEventApplicationsStatus
                    } else {
                        throw cause instanceof RuntimeException re ? re : ex;
                    }
                }
            }

            return new EventApplicationsStatusResponse(
                    e.getId(),
                    event.getId(),
                    event.getName(),
                    firstEventImageUrl,
                    event.getStartDate(),
                    e.getStatus()
            );

        });
    }

    @Override
    public CheckEventCheckInCodeResponse checkEventCheckInCode(CheckEventCheckInCodeRequest request) {
        UUID volunteerId = currentUserProvider.getId();

        OffsetDateTime checkInTime = OffsetDateTime.now();

        //find today's vol event application
        EventApplication eventApplication = eventApplicationRepository
                .findEventApplicationByVolunteerIdAndSessionDate(volunteerId, LocalDate.now(), request.getCheckInCode(), checkInTime);

        //check if event application exists
        if (eventApplication == null) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED);
        }

        //find today's vol event session
        UUID eventSessionId = eventApplication.getSession().getId();

        CheckInLog checkInLog = checkInLogRepository
                .findByEventApplicationId(eventApplication.getId());

        //check if vol check-in log exists
        if (checkInLog != null) {
            throw new AppException(EventErrorCode.ALREADY_CHECKED_IN);
        }

        EventSession eventSession = eventSessionRepository.findById(eventSessionId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED)
        );

        //find today's vol event
        UUID eventId = eventSession.getEvent().getId();

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check if event is in status ONGOING
        if (!event.getStatus().equals(EEventStatus.ONGOING)) {
            throw new AppException(EventErrorCode.EVENT_NOT_ONGOING);
        }

        //check if check-in code is correct
        if (!eventSession.getCheckInCode().equals(request.getCheckInCode())) {
            throw new AppException(EventErrorCode.EVENT_CHECK_IN_CODE_NOT_MATCH);
        }

        return CheckEventCheckInCodeResponse.builder()
                .eventId(eventId)
                .eventSessionId(eventSessionId)
                .build();
    }

    @Override
    public void quickCheckInEvent(QuickCheckInEventRequest request) {
        OffsetDateTime checkInTime = OffsetDateTime.now();

        UUID volunteerId = currentUserProvider.getId();

        EventApplication eventApplication = eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, UUID.fromString(request.getEventSessionId()));

        //check if event application exists
        if (eventApplication == null) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED);
        }

        CheckInLog checkInLog = checkInLogRepository
                .findByEventApplicationId(eventApplication.getId());

        //check if vol check-in log exists
        if (checkInLog != null) {
            throw new AppException(EventErrorCode.ALREADY_CHECKED_IN);
        }

        //find today's vol event
        EventSession eventSession = eventSessionRepository.findById(UUID.fromString(request.getEventSessionId())).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED)
        );

        //Check if event session is started
        if(!checkInTime.isAfter(eventSession.getStartDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_NOT_STARTED);
        }

        //Check if event session is ended
        if(!checkInTime.isBefore(eventSession.getEndDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_ENDED);
        }

        Event event = eventSession.getEvent();

        //Check if current vol applied event exist
        if(event == null) {
            throw new AppException(EventErrorCode.EVENT_NOT_EXISTED);
        }

        //check if event is in status ONGOING
        if(!event.getStatus().equals(EEventStatus.ONGOING)) {
            throw new AppException(EventErrorCode.EVENT_NOT_ONGOING);
        }

        //check if current vol position is in check-in location
        Point currentPosition = GeoUtils.toPoint(request.getCurrentPlaceLat(), request.getCurrentPlaceLng());

        double distance = GeoUtils.distanceMeters(currentPosition,event.getCheckInLocation());

        //Check if current user's position is in check-in location
        if (distance > event.getCheckInAccuracyMeters()) {
            throw new AppException(EventErrorCode.EVENT_CHECK_IN_OUT_OF_RANGE);
        }

        //check if current user's device is not used to check in by another user
        boolean existsByDevice = checkInLogRepository
                .existsByDevice(request.getDeviceId(), request.getApVersion(), request.getOsVersion());

        if(existsByDevice) {
            throw new AppException(EventErrorCode.DEVICE_ALREADY_CHECKED_IN);
        }

        //get logged in vol
        Volunteer volunteer = volunteerRepository.findById(volunteerId).orElseThrow(
                () -> new AppException(VolunteerErrorCode.VOLUNTEER_NOT_EXISTED)
        );

        //todo handle what if current user's device is not match with stored user's device
        //check if current user's device is match with stored user's device
        if(request.getDeviceId().equals(volunteer.getDeviceId())) {

            //save new check-in log into db
            CheckInLog newCheckInLog = new CheckInLog();
            newCheckInLog.setEventApplication(eventApplication);
            newCheckInLog.setDeviceId(request.getDeviceId());
            newCheckInLog.setApVersion(request.getApVersion());
            newCheckInLog.setOsVersion(request.getOsVersion());
            newCheckInLog.setCheckInLocation(currentPosition);
            newCheckInLog.setCheckInTime(checkInTime);
            checkInLogRepository.save(newCheckInLog);
        }
    }

    @Override
    public void checkOutEvent(CheckOutEventRequest request) {
        OffsetDateTime checkInTime = OffsetDateTime.now();

        UUID volunteerId = currentUserProvider.getId();

        EventApplication eventApplication = eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, UUID.fromString(request.getEventSessionId()));

        //check if event application exists
        if (eventApplication == null) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED);
        }

        //get current vol check-in log
        CheckInLog checkInLog = checkInLogRepository
                .findByEventApplicationId(eventApplication.getId());

        //check if vol check-in log exists
        if (checkInLog == null) {
            throw new AppException(EventErrorCode.EVENT_SESSION_NOT_CHECKED_IN);
        }

        //find today's vol event
        EventSession eventSession = eventSessionRepository.findById(UUID.fromString(request.getEventSessionId())).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED)
        );

        //Check if event session is started
        if(!checkInTime.isAfter(eventSession.getStartDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_NOT_STARTED);
        }

        //Check if event session is ended
        if(!checkInTime.isBefore(eventSession.getEndDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_ENDED);
        }

        Event event = eventSession.getEvent();

        //Check if current vol applied event exist
        if(event == null) {
            throw new AppException(EventErrorCode.EVENT_NOT_EXISTED);
        }

        //check if event is in status ONGOING
        if(!event.getStatus().equals(EEventStatus.ONGOING)) {
            throw new AppException(EventErrorCode.EVENT_NOT_ONGOING);
        }

        //check if current vol position is in check-in location
        Point currentPosition = GeoUtils.toPoint(request.getCurrentPlaceLat(), request.getCurrentPlaceLng());

        double distance = GeoUtils.distanceMeters(currentPosition,event.getCheckInLocation());

        //Check if current user's position is in check-in location
        if (distance > event.getCheckInAccuracyMeters()) {
            throw new AppException(EventErrorCode.EVENT_CHECK_IN_OUT_OF_RANGE);
        }

        //check if current user's device is match with checked-in user's device
        boolean existsByDeviceAndVolunteerId = checkInLogRepository
                .existsByDeviceAndEventApplication(request.getDeviceId(),
                        request.getApVersion(), request.getOsVersion(), eventApplication.getId());

        if(!existsByDeviceAndVolunteerId) {
            throw new AppException(EventErrorCode.DEVICE_NOT_CHECKED_IN);
        }

        //Save credit hour
        Duration duration = Duration.between(checkInLog.getCheckInTime(), checkInTime);
        double creditHour = duration.toHours() + (duration.toMinutesPart() / 60.0);

        //Calculate total credit hour today
        List<EventApplication> allEventApplicationToday = eventApplicationRepository
                .findAllByVolunteerIdAndSessionDate(volunteerId, LocalDate.now());

        short totalCreditHourToday = 0;

        for(EventApplication ea: allEventApplicationToday) {
            totalCreditHourToday += ea.getCreditHour();
        }

        //Check if total credit hour today is less than 12
        if(12 - totalCreditHourToday >= creditHour) {
            eventApplication.setCreditHour((short) creditHour);
        } else {
            eventApplication.setCreditHour((short) (12 - totalCreditHourToday));
        }

        //Set status of event application to COMPLETED
        eventApplication.setStatus(EEventApplicationStatus.COMPLETED);

        eventApplicationRepository.save(eventApplication);
    }
}
