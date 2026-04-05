package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventClaimStatus;
import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.request.EventClaimVerifyRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventClaim;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventClaimRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.impl.EventClaimServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventClaimServiceTest {

    @Mock
    EventSessionRepository eventSessionRepository;

    @Mock
    EventApplicationRepository eventApplicationRepository;

    @Mock
    EventClaimRepository eventClaimRepository;

    @Mock
    VolunteerRepository volunteerRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    StorageService storageService;

    @Mock
    StoragePathGenerator storagePathGenerator;

    @InjectMocks
    EventClaimServiceImpl service;

    UUID volunteerId;
    UUID eventId;
    UUID hostId;
    UUID sessionId;

    @BeforeEach
    void setup() {

        volunteerId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        hostId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    private ClaimEventHourRequest validClaimEventHourRequest(){
        ClaimEventHourRequest request = new ClaimEventHourRequest();
        request.setEventSessionId(sessionId.toString());
        request.setHonorHours((short) 8);
        request.setReason("Reason");
        request.setDetailReason("DetailReason");
        request.setEvidences("img1 img2 img3");
        return request;
    }

    // ==== claimEventHour ===================================
    // ===== TC1 =====
    @Test
    void claimEventHour_success() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventClaimRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        when(storagePathGenerator.eventClaimImages(any(), anyInt(), any()))
                .thenAnswer(inv -> "path-" + inv.getArgument(1));

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        ClaimEventHourRequest request = validClaimEventHourRequest();

        ClaimEventHourResponse response = service.claimEventHour(request);

        assertNotNull(response);
        assertEquals(3, response.getEvidencesUploadUrls().size());

        verify(eventClaimRepository).save(any(EventClaim.class));
    }

    // ===== TC2 =====
    @Test
    void claimEventHour_application_not_exist() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(null);

        ClaimEventHourRequest request = validClaimEventHourRequest();

        assertThrows(AppException.class,
                () -> service.claimEventHour(request));
    }

    // ===== TC3 =====
    @Test
    void claimEventHour_session_not_exist() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ClaimEventHourRequest request = validClaimEventHourRequest();

        assertThrows(AppException.class,
                () -> service.claimEventHour(request));
    }

    // ===== TC4 =====
    @Test
    void claimEventHour_out_of_claim_time() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setEndDateTime(OffsetDateTime.now().minusDays(10)); // > 7 days

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ClaimEventHourRequest request = validClaimEventHourRequest();

        assertThrows(AppException.class,
                () -> service.claimEventHour(request));
    }

    // ===== TC5 =====
    @Test
    void claimEventHour_already_claimed() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setId(sessionId);
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventClaimRepository.findByEventApplicationId(applicationId))
                .thenReturn(new EventClaim());

        ClaimEventHourRequest request = validClaimEventHourRequest();

        assertThrows(AppException.class,
                () -> service.claimEventHour(request));
    }

    // ===== TC6 =====
    @Test
    void claimEventHour_limit_5_evidences() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventClaimRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        when(storagePathGenerator.eventClaimImages(any(), anyInt(), any()))
                .thenAnswer(inv -> "path-" + inv.getArgument(1));

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        ClaimEventHourRequest request = validClaimEventHourRequest();
        request.setEvidences("a b c d e f"); // >4

        ClaimEventHourResponse response = service.claimEventHour(request);

        assertEquals(5, response.getEvidencesUploadUrls().size());
    }

    // ===== TC7 =====
    @Test
    void claimEventHour_async_upload_fail() {

        UUID applicationId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        EventApplication app = new EventApplication();
        app.setId(applicationId);

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        when(eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, sessionId))
                .thenReturn(app);

        when(eventSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(eventClaimRepository.findByEventApplicationId(applicationId))
                .thenReturn(null);

        when(storagePathGenerator.eventClaimImages(any(), anyInt(), any()))
                .thenReturn("path");

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("upload fail"));

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(failedFuture);

        ClaimEventHourRequest request = validClaimEventHourRequest();

        assertThrows(RuntimeException.class,
                () -> service.claimEventHour(request));
    }

    // ==== verifyEventClaim ===================================
    // ===== TC1 =====
    @Test
    void verifyEventClaim_approve_success() {

        UUID claimId = UUID.randomUUID();

        EventClaim claim = new EventClaim();
        claim.setId(claimId);
        claim.setStatus(EEventClaimStatus.PENDING);
        claim.setHonorHour((short) 5);

        Volunteer volunteer = new Volunteer();
        volunteer.setHonorScore(10);

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        EventApplication app = new EventApplication();
        app.setVolunteer(volunteer);
        app.setSession(session);

        claim.setEventApplication(app);

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.of(claim));

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();
        request.setApprove(true);

        service.verifyEventClaim(claimId, request);

        verify(volunteerRepository).save(any(Volunteer.class));
        verify(eventApplicationRepository).save(any(EventApplication.class));
        verify(eventClaimRepository, atLeastOnce()).save(any(EventClaim.class));
    }

    // ===== TC2 =====
    @Test
    void verifyEventClaim_not_found() {

        UUID claimId = UUID.randomUUID();

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.empty());

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();

        assertThrows(AppException.class,
                () -> service.verifyEventClaim(claimId, request));
    }

    // ===== TC3 =====
    @Test
    void verifyEventClaim_already_resolved() {

        UUID claimId = UUID.randomUUID();

        EventClaim claim = new EventClaim();
        claim.setStatus(EEventClaimStatus.APPROVED);

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.of(claim));

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();

        assertThrows(AppException.class,
                () -> service.verifyEventClaim(claimId, request));
    }

    // ===== TC4 =====
    @Test
    void verifyEventClaim_out_of_verify_time() {

        UUID claimId = UUID.randomUUID();

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(10));

        EventApplication app = new EventApplication();
        app.setSession(session);

        EventClaim claim = new EventClaim();
        claim.setStatus(EEventClaimStatus.PENDING);
        claim.setEventApplication(app);

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.of(claim));

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();
        request.setApprove(true);

        assertThrows(AppException.class,
                () -> service.verifyEventClaim(claimId, request));
    }

    // ===== TC5 =====
    @Test
    void verifyEventClaim_approve_but_volunteer_null() {

        UUID claimId = UUID.randomUUID();

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        EventApplication app = new EventApplication();
        app.setSession(session);
        app.setVolunteer(null);

        EventClaim claim = new EventClaim();
        claim.setStatus(EEventClaimStatus.PENDING);
        claim.setEventApplication(app);

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.of(claim));

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();
        request.setApprove(true);

        service.verifyEventClaim(claimId, request);

        verify(volunteerRepository, never()).save(any());
        verify(eventApplicationRepository, never()).save(any());
    }

    // ===== TC6 =====
    @Test
    void verifyEventClaim_reject() {

        UUID claimId = UUID.randomUUID();

        EventSession session = new EventSession();
        session.setEndDateTime(OffsetDateTime.now().minusDays(1));

        EventApplication app = new EventApplication();
        app.setSession(session);

        EventClaim claim = new EventClaim();
        claim.setStatus(EEventClaimStatus.PENDING);
        claim.setEventApplication(app);

        when(eventClaimRepository.findById(claimId))
                .thenReturn(Optional.of(claim));

        EventClaimVerifyRequest request = new EventClaimVerifyRequest();
        request.setApprove(false);

        service.verifyEventClaim(claimId, request);

        verify(volunteerRepository, never()).save(any());
        verify(eventApplicationRepository, never()).save(any());

        verify(eventClaimRepository, atLeastOnce()).save(any(EventClaim.class));
    }
}
