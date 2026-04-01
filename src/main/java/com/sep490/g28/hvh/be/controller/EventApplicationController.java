package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.service.EventApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventApplicationController {

    EventApplicationService eventApplicationService;

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/event-session/{sessionId}/apply")
    ResponseEntity<Void> applyEvent(@PathVariable UUID sessionId) {
        eventApplicationService.applyEventSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST') and @eventApplicationAuthorizer.isHostOfEventApplication(#applicationId)")
    @PutMapping("/host/event-applications/{applicationId}/approve")
    ResponseEntity<Void> approveApplication(@PathVariable UUID applicationId) {
        eventApplicationService.approveApplication(applicationId);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("hasRole('HOST') and @eventApplicationAuthorizer.isHostOfEventApplication(#id)")
    @PutMapping("/host/event-applications/{id}/reject")
    ResponseEntity<Void> rejectApplication(
            @PathVariable UUID id,
            @RequestBody @Valid RejectApplicationRequest request
    ) {
        eventApplicationService.rejectApplication(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/event-session/{id}/registered-participants")
    ResponseEntity<EventApplicationsResponse> getRegisteredParticipants(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @PathVariable(name = "id")
            @org.hibernate.validator.constraints.UUID(message = "INVALID_UUID")
            String inputId) {

        UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(eventApplicationService.getRegisteredParticipants(pageNumber, pageSize, id));
    }

    @PreAuthorize("hasRole('VOL') and @eventApplicationAuthorizer.isVolunteerOfEventApplication(#id)")
    @PutMapping("/vol/event-applications/{id}/cancel")
    ResponseEntity<Void> cancelApplication(
            @PathVariable UUID id
    ) {
        eventApplicationService.cancelApplication(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('VOL')")
    @GetMapping("/vol/event-applications")
    ResponseEntity<Page<EventApplicationsStatusResponse>> getEventApplicationsStatus(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(defaultValue = "PENDING")
            String status) {

        return ResponseEntity.ok(eventApplicationService.getEventApplicationsStatus(pageNumber, pageSize, status));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/events/check-event-check-in-code")
    public ResponseEntity<CheckEventCheckInCodeResponse> checkEventCheckInCode(@Valid @RequestBody CheckEventCheckInCodeRequest request) {
        return ResponseEntity.ok(eventApplicationService.checkEventCheckInCode(request));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/events/quick-check-in")
    public ResponseEntity<Void> quickCheckIn(@Valid @RequestBody QuickCheckInEventRequest request) {
        eventApplicationService.quickCheckInEvent(request);
        return ResponseEntity.ok().build();
    }
}
