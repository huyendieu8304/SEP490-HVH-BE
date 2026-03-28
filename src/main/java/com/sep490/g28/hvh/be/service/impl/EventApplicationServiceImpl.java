package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.EventApplicationService;
import com.sep490.g28.hvh.be.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        if (!event.getStatus().equals(EEventStatus.RECRUITING)){
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
        if (session.getExpectedVolAmount() == session.getApprovedApplicationCount()){
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
        if (!eventApplication.getStatus().equals(EEventApplicationStatus.PENDING)){
             throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_PENDING);
        }
        //check the expected amount
        EventSession eventSession = eventApplication.getSession();
        if (eventSession.getApprovedApplicationCount() >= eventSession.getExpectedVolAmount()){
            throw new AppException(EventErrorCode.EVENT_SESSION_FULL);
        }

        //check the status of the event
        Event event = eventSession.getEvent();
        if (event.getStatus() != EEventStatus.RECRUITING){
            throw new AppException(EventErrorCode.EVENT_NOT_RECRUITING);
        }

        eventApplication.setStatus(EEventApplicationStatus.APPROVED);
        eventApplicationRepository.save(eventApplication);

        eventSession.setApprovedApplicationCount(eventSession.getApprovedApplicationCount()+1);
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
        if (!eventApplication.getStatus().equals(EEventApplicationStatus.PENDING)){
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_PENDING);
        }

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
        if (!EEventStatus.volunteerCanCancelledApplication(event.getStatus())){
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCEL);
        }

        //check application status, only PENDING and APPROVED can cancel
        if (eventApplication.getStatus().equals(EEventApplicationStatus.CANCELLED)
                || eventApplication.getStatus().equals(EEventApplicationStatus.REJECTED)){
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCEL);
        }
        LocalDate today = LocalDate.now();

        // not allow to cancel on the date or after the session date
        if (!today.isBefore(eventApplication.getSessionDate())) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_CANNOT_CANCEL);
        }

        boolean isMinusScore = false;
        //application is approved -> check the event timeline
        if (eventApplication.getStatus().equals(EEventApplicationStatus.APPROVED)){
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
            eventSession.setApprovedApplicationCount(eventSession.getApprovedApplicationCount()-1);
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
                Sort.by(Sort.Direction.ASC, "createdAt")
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

                    if(e.getVolunteer() != null) {

                        Volunteer volunteer = e.getVolunteer();

                        volunteerId = volunteer.getId();
                        email = volunteer.getEmail();
                        phone = volunteer.getPhone();
                        nickName = volunteer.getNickname();
                        name = volunteer.getFullName();

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
                            avatarUrl
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
    public List<EventApplication> cancelAllApplicationsToEvent(Event event) {
        //update all the applications of the volunteer to CANCELLED status
        List<EventSession> eventSessions = event.getSessions();
        List<UUID> sessionIds = eventSessions.stream().map(EventSession::getId).toList();
        List<EventApplication> eventApplications =  eventApplicationRepository.cancelApplicationsBySessions(sessionIds);
        log.info("All the applications of volunteer has been cancelled");
        return eventApplications;
    }
}
