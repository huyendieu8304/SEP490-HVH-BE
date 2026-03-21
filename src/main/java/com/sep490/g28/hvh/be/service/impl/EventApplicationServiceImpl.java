package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.EventApplicationService;
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

    @Transactional
    @Override
    public void applyEventSession(UUID sessionId) {
        //check session existed?
        EventSession session = eventSessionRepository.findById(sessionId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED));

        Event event = session.getEvent();
        //only allow application when the event status is RECRUITING
        if (!event.getStatus().equals(EEventStatus.RECRUITING)){
            throw new AppException(EventErrorCode.EVENT_NOT_RECRUITING);
        }

        //check registration deadline
        if (LocalDate.now().isAfter(event.getRecruitmentEndDate())) {
            throw new AppException(EventErrorCode.EVENT_RECRUITMENT_CLOSED);
        }

        UUID volunteerId = currentUserProvider.getId();
        //Have ever the volunteer applied for this session yet?
        if (eventApplicationRepository.getEventApplicationsByVolunteerIdAndSessionId(volunteerId, sessionId).isPresent()) {
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
        if (event.isAutoApprove()){
            eventApplication.setStatus(EEventApplicationStatus.APPROVED);
            session.setApprovedApplicationCount(session.getApprovedApplicationCount()+1);
            eventSessionRepository.save(session);
        } else {
            eventApplication.setStatus(EEventApplicationStatus.PENDING);
        }
        eventApplicationRepository.save(eventApplication);
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
}
