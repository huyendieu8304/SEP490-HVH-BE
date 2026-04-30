package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventapplication.request.*;
import com.sep490.g28.hvh.be.dto.eventapplication.response.AccountCheckInStatusResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CompletedApplicationResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.ActualParticipantResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventApplicationController {

    EventApplicationService eventApplicationService;

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-sessions/{sessionId}/apply")
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
    @GetMapping("/host/event-sessions/{id}/registered-participants")
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
            String inputId
    ) {

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

            @RequestParam()
            String status) {

        return ResponseEntity.ok(eventApplicationService.getEventApplicationsStatus(pageNumber, pageSize, status));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-applications/check-event-check-in-code")
    public ResponseEntity<CheckEventCheckInCodeResponse> checkEventCheckInCode(@Valid @RequestBody CheckEventCheckInCodeRequest request) {
        return ResponseEntity.ok(eventApplicationService.checkEventCheckInCode(request));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-applications/quick-check-in")
    public ResponseEntity<Void> quickCheckIn(@Valid @RequestBody QuickCheckInEventRequest request) {
        eventApplicationService.quickCheckInEvent(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-applications/check-out")
    public ResponseEntity<Void> checkOutEvent(@Valid @RequestBody CheckOutEventRequest request) {
        eventApplicationService.checkOutEvent(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST') and @eventSessionAuthorizer.isHostOfEventSession(#sessionId)")
    @GetMapping("/host/event-sessions/{sessionId}/actual-participants")
    public ResponseEntity<Page<ActualParticipantResponse>> getActualParticipants(
            @PathVariable UUID sessionId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize
    ){

        return ResponseEntity.ok(eventApplicationService.getActualParticipants(sessionId, pageNumber, pageSize));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-applications/face-check-in")
    public ResponseEntity<Void> faceCheckInEvent(
            @Valid
            @RequestPart("request")
            FaceCheckInEventRequest request,

            @RequestPart("file")
            MultipartFile file
    ) {
        eventApplicationService.faceCheckInEvent(request, file);
        return ResponseEntity.ok().build();
    }

    //get COMPLETED application
    @PreAuthorize("hasRole('HOST') and @eventSessionAuthorizer.isHostOfEventSession(#sessionId)")
    @GetMapping("/host/event-sessions/{sessionId}/completed-applications")
    public ResponseEntity<Page<CompletedApplicationResponse>> getCompletedApplications(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @PathVariable UUID sessionId
    ){
        return ResponseEntity.ok(eventApplicationService.getCompletedApplications(sessionId, pageNumber, pageSize));
    }

    @PreAuthorize("hasRole('VOL')")
    @GetMapping("/vol/event-applications/check-in-status")
    public ResponseEntity<AccountCheckInStatusResponse> getAccountCheckInStatus() {
        return ResponseEntity.ok(eventApplicationService.getAccountCheckInStatus());
    }
}
