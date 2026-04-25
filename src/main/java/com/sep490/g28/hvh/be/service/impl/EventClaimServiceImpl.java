package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventClaimStatus;
import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.request.VerifyEventClaimRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimDetailResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimSimpleResponse;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventClaim;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventClaimRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.EventClaimService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventClaimServiceImpl implements EventClaimService {

    EventApplicationRepository eventApplicationRepository;
    EventClaimRepository eventClaimRepository;
    EventSessionRepository eventSessionRepository;
    VolunteerRepository volunteerRepository;

    StorageService storageService;
    StoragePathGenerator storagePathGenerator;
    CurrentUserProvider currentUserProvider;

    @Override
    public ClaimEventHourResponse claimEventHour(ClaimEventHourRequest request) {
        OffsetDateTime claimTime = OffsetDateTime.now();

        UUID volunteerId = currentUserProvider.getId();

        EventApplication eventApplication = eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, UUID.fromString(request.getEventSessionId()));

        //check if event application exists
        if (eventApplication == null) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED);
        }

        EventSession eventSession = eventSessionRepository.findById(UUID.fromString(request.getEventSessionId())).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED)
        );

        //Calculate duration between session end time and claim time
        Duration claimDuration = Duration.between(eventSession.getEndDateTime(), claimTime);
        long days = claimDuration.toDays();
        boolean hasTimeRemainder = !claimDuration.minusDays(days).isZero();
        if (hasTimeRemainder && claimDuration.isNegative()) {
            days--;
        } else if (hasTimeRemainder) {
            days++;
        }

        //check if duration between session end time and claim time <= 7 days
        if (days > 7) {
            throw new AppException(EventErrorCode.EVENT_CLAIM_OUT_OF_CLAIM_TIME);
        }

        //Calculate duration between session start time and session end time
        Duration sessionDuration = Duration.between(eventSession.getStartDateTime(), eventSession.getEndDateTime());
        double totalCreditHour = sessionDuration.toHours() + (sessionDuration.toMinutesPart() / 60.0);

        //check if honor hour is not exceeds true credit hour of session
        if(request.getHonorHours() > (short) Math.round(totalCreditHour)) {
            throw new AppException(EventErrorCode.EVENT_CLAIM_INVALID_HONOR_HOUR_REQUEST);
        }

        //check if session has been claimed
        EventClaim eventClaim = eventClaimRepository.findByEventApplicationId(eventApplication.getId());
        if (eventClaim != null) {
            throw new AppException(EventErrorCode.EVENT_SESSION_ALREADY_CLAIMED);
        }

        //get the evidences image paths in storage
        String[] evidences = request.getEvidences().split("\\s+");
        List<String> evidencesPathsList = new ArrayList<>();
        int legal_order = 1;
        for (String evidence : evidences) {
            String evidencePath = storagePathGenerator
                    .eventClaimImages(eventSession.getEvent().getId(), eventApplication.getId(), legal_order++, evidence);
            evidencesPathsList.add(evidencePath);
            if (legal_order == 6) {
                break;
            }
        }

        StringBuilder evidencesPathsSB = new StringBuilder();
        for (String evidencePath : evidencesPathsList) {
            evidencesPathsSB.append(evidencePath).append(" ");
        }
        String evidencesPaths = evidencesPathsSB.toString().trim();

        //get upload urls for claim's evidences
        List<CompletableFuture<String>> evidencesFutures = new ArrayList<>();
        for (String evidencePath : evidencesPathsList) {
            CompletableFuture<String> evidenceFuture =
                    storageService.getUploadUrlAsync(evidencePath);
            evidencesFutures.add(evidenceFuture);
        }

        try {
            CompletableFuture.allOf(evidencesFutures.toArray(new CompletableFuture[0])).join();

        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }

        List<String> evidencesUploadUrl = new ArrayList<>();
        for (CompletableFuture<String> evidencesFuture : evidencesFutures) {
            evidencesUploadUrl.add(evidencesFuture.join());
        }

        //create new eventClaim in db
        EventClaim newEventClaim = new EventClaim();
        newEventClaim.setEventApplication(eventApplication);
        newEventClaim.setHonorHour(request.getHonorHours());
        newEventClaim.setReason(request.getReason());
        newEventClaim.setDetailReason(request.getDetailReason());
        newEventClaim.setEvidences(evidencesPaths);
        eventClaimRepository.save(newEventClaim);

        return ClaimEventHourResponse.builder()
                .evidencesUploadUrls(evidencesUploadUrl)
                .build();
    }

    @Override
    public Page<EventClaimSimpleResponse> getEventClaims(int pageNumber, int pageSize, UUID eventId, UUID sessionId) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventClaim> page = eventClaimRepository.findByEventIdAndSessionId(eventId, sessionId, pageable);

        //check if no event claim
        if (page.getContent().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, page.getTotalElements());
        }

        return page.map(e -> {

            UUID volunteerId = null;
            String nickName = null;
            String name = null;
            String avatarUrl = null;
            int creditScore = 0;
            int honorScore = 0;

            EventApplication eventApplication = e.getEventApplication();

            //check if the event application linked with a volunteer
            if (eventApplication.getVolunteer() != null) {

                Volunteer volunteer = eventApplication.getVolunteer();

                volunteerId = volunteer.getId();
                nickName = volunteer.getNickname();
                name = volunteer.getFullName();
                creditScore = volunteer.getCreditScore();
                honorScore = volunteer.getHonorScore();


                //get signed URL of volunteer avatar
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

            return new EventClaimSimpleResponse(
                    e.getId(),
                    volunteerId,
                    nickName,
                    name,
                    avatarUrl,
                    creditScore,
                    honorScore,
                    e.getHonorHour(),
                    e.getReason(),
                    e.getCreatedAt()
            );
        });

    }

    @Override
    public EventClaimDetailResponse getEventClaimDetail(UUID claimId) {
        EventClaim eventClaim = eventClaimRepository.findById(claimId)
                .orElseThrow(() -> new AppException(EventErrorCode.EVENT_CLAIM_NOT_FOUND));

        EventApplication eventApplication = eventClaim.getEventApplication();

        UUID volunteerId = null;
        String email = null;
        String phone = null;
        String nickName = null;
        String name = null;
        String avatarUrl = null;
        String address = null;
        int creditScore = 0;
        int honorScore = 0;
        List<String> evidencesUrls = new ArrayList<>();

        CompletableFuture<String> avatarFuture = null;

        //check if the event application linked with a volunteer
        if (eventApplication.getVolunteer() != null) {

            Volunteer volunteer = eventApplication.getVolunteer();

            volunteerId = volunteer.getId();
            email = volunteer.getEmail();
            phone = volunteer.getPhone();
            nickName = volunteer.getNickname();
            name = volunteer.getFullName();
            address = volunteer.getAddress();
            creditScore = volunteer.getCreditScore();
            honorScore = volunteer.getHonorScore();

            //check if volunteer has avatar
            if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {
                avatarFuture = storageService.getSignedUrlAsync(volunteer.getAvatarUrl());
            }
        }
        //get signed URL of evidences files and avatar of volunteer (if exists)
        List<CompletableFuture<String>> evidencesFutures = new ArrayList<>();
        if (eventClaim.getEvidences() != null) {
            String[] evidences = eventClaim.getEvidences().split("\\s+");
            List<String> evidencesList = new ArrayList<>(Arrays.asList(evidences));
            for (String evidence : evidencesList) {
                CompletableFuture<String> legalDocumentFuture =
                        storageService.getSignedUrlAsync(evidence);
                evidencesFutures.add(legalDocumentFuture);
            }
        }

        try {

            if (avatarFuture != null) {
                CompletableFuture.allOf(avatarFuture).join();
                avatarUrl = avatarFuture.join();
            }
            CompletableFuture.allOf(evidencesFutures.toArray(new CompletableFuture[0])).join();


            for (CompletableFuture<String> evidencesFuture : evidencesFutures) {
                evidencesUrls.add(evidencesFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo handle here
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        return EventClaimDetailResponse.builder()
                .id(eventClaim.getId())
                .sessionId(eventApplication.getSession().getId())
                .volunteerId(volunteerId)
                .email(email)
                .phone(phone)
                .nickName(nickName)
                .name(name)
                .avatarUrl(avatarUrl)
                .address(address)
                .creditScore(creditScore)
                .honorScore(honorScore)
                .honorHours(eventClaim.getHonorHour())
                .reason(eventClaim.getReason())
                .detailReason(eventClaim.getDetailReason())
                .evidencesUrls(evidencesUrls)
                .createdAt(eventClaim.getCreatedAt())
                .build();
    }

    @Override
    public void verifyEventClaim(UUID claimId, VerifyEventClaimRequest request) {
        OffsetDateTime verifyTime = OffsetDateTime.now();

        EventClaim eventClaim = eventClaimRepository.findById(claimId)
                .orElseThrow(() -> new AppException(EventErrorCode.EVENT_CLAIM_NOT_FOUND));

        if (!eventClaim.getStatus().equals(EEventClaimStatus.PENDING)) {
            throw new AppException(EventErrorCode.EVENT_CLAIM_ALREADY_RESOLVED);
        }

        EventApplication eventApplication = eventClaim.getEventApplication();

        //Calculate duration between session end time and current verify time
        Duration duration = Duration.between(eventApplication.getSession().getEndDateTime(), verifyTime);
        long days = duration.toDays();
        boolean hasTimeRemainder = !duration.minusDays(days).isZero();
        if (hasTimeRemainder && duration.isNegative()) {
            days--;
        } else if (hasTimeRemainder) {
            days++;
        }

        //check if duration between session end time and current verify time <= 9 days
        if (days > 9) {
            throw new AppException(EventErrorCode.EVENT_CLAIM_OUT_OF_VERIFY_TIME);
        }

        if (Boolean.TRUE.equals(request.getApprove())) {
            if (eventApplication.getVolunteer() != null) {
                Volunteer volunteer = eventApplication.getVolunteer();
                volunteer.setHonorScore(volunteer.getHonorScore() + eventClaim.getHonorHour());
                volunteerRepository.save(volunteer);

                eventApplication.setHonorHour(eventClaim.getHonorHour());
                eventApplication.setStatus(EEventApplicationStatus.COMPLETED);
                eventApplicationRepository.save(eventApplication);

                eventClaim.setStatus(EEventClaimStatus.APPROVED);
                eventClaimRepository.save(eventClaim);
            }
        } else {
            eventClaim.setStatus(EEventClaimStatus.REJECTED);
            eventClaimRepository.save(eventClaim);
        }
    }


}
