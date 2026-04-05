package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.request.EventClaimVerifyRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimDetailResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimSimpleResponse;
import com.sep490.g28.hvh.be.service.EventClaimService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventClaimController {
    EventClaimService eventClaimService;

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-claims")
    public ResponseEntity<ClaimEventHourResponse> claimEventHours(@RequestBody @Valid ClaimEventHourRequest request) {

        return ResponseEntity.ok(eventClaimService.claimEventHour(request));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/event-claims/{eventId}")
    public ResponseEntity<Page<EventClaimSimpleResponse>> getEventClaims(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @PathVariable(name = "eventId")
            @UUID(message = "INVALID_UUID")
            String inputEventId,

            @RequestParam(required = false)
            @UUID(message = "INVALID_UUID")
            String inputSessionId
    ) {
        java.util.UUID eventId = java.util.UUID.fromString(inputEventId);
        java.util.UUID sessionId = java.util.UUID.fromString(inputSessionId);
        return ResponseEntity.ok(eventClaimService.getEventClaims(pageNumber, pageSize, eventId, sessionId));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/event-claims/{claimId}")
    public ResponseEntity<EventClaimDetailResponse> getEventClaimDetail(
            @PathVariable(name = "claimId")
            @UUID(message = "INVALID_UUID")
            String claimId
    ) {
        java.util.UUID id = java.util.UUID.fromString(claimId);
        return ResponseEntity.ok(eventClaimService.getEventClaimDetail(id));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/event-claims/{claimId}/verify")
    public ResponseEntity<Void> verifyEventClaim(
            @PathVariable(name = "claimId")
            @UUID(message = "INVALID_UUID")
            String inputId,

            @RequestBody
            @Valid
            EventClaimVerifyRequest request) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        eventClaimService.verifyEventClaim(id, request);
        return ResponseEntity.ok().build();
    }
}
