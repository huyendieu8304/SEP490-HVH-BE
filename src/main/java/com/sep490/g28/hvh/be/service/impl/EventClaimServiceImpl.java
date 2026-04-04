package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventClaim;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventClaimRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.EventClaimService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

        //Check if duration between event end time and claim time <= 7 days
        Duration duration = Duration.between(eventSession.getEndDateTime(), claimTime);
        long days = duration.toDays();
        boolean hasTimeRemainder = !duration.minusDays(days).isZero();
        if (hasTimeRemainder && duration.isNegative()) {
            days--;
        } else if (hasTimeRemainder) {
            days++;
        }

        if (days > 7) {
//            throw new AppException(EventErrorCode.EVENT_CLAIM_OUT_OF_TIME);
        }

        //check if session has been claimed
        EventClaim eventClaim = eventClaimRepository.findByEventApplicationId(eventApplication.getId());
        if (eventClaim != null) {
//            throw new AppException(EventErrorCode.ALREADY_CLAIMED);
        }

        //get the path in storage
        String[] evidences = request.getEvidences().split("\\s+");
        List<String> evidencesPathsList = new ArrayList<>();
        int legal_order = 1;
        for (String evidence : evidences) {
            String evidencePath = storagePathGenerator.eventClaimImages(eventApplication.getId(), legal_order++, evidence);
            evidencesPathsList.add(evidencePath);
            if (legal_order == 5) {
                break;
            }
        }

        StringBuilder evidencesPathsSB = new StringBuilder();
        for (String evidencePath : evidencesPathsList) {
            evidencesPathsSB.append(evidencePath).append(" ");
        }
        String evidencesPaths = evidencesPathsSB.toString().trim();

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
                .evidencesUrls(evidencesUploadUrl)
                .build();
    }
}
