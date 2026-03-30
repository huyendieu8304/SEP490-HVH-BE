package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.event.request.*;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.service.EventService;
import com.sep490.g28.hvh.be.validation.EventStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventController {

    EventService eventService;

    @GetMapping("/event/new-feeds")
    public ResponseEntity<EventFeedResponse> getEventNewFeeds(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(defaultValue = "true")
            boolean refresh,

            @RequestParam(required = false)
            String name,

            @RequestParam(required = false)
            String address,

            @RequestParam(required = false)
            LocalDate startDate,

            @RequestParam(required = false)
            LocalDate endDate,

            @RequestParam(required = false)
            List<Short> activitySubDomainIds
    ) {
        return ResponseEntity.ok(eventService.getEventFeeds(pageNumber, pageSize, refresh, name, address, startDate, endDate, activitySubDomainIds));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/event/draft")
    ResponseEntity<EditEventResponse> draftEvent(@RequestBody @Valid EditEventRequest request) {

        return ResponseEntity.ok(eventService.draftEvent(request));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/event/submit")
    ResponseEntity<EditEventResponse> submitEvent(@RequestBody @Valid EditEventRequest request) {

        return ResponseEntity.ok(eventService.submitEvent(request));
    }

    @GetMapping("/event/event-details/{id}")
    public ResponseEntity<EventDetailsResponse> getEventDetails(
            @PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId
    ) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(eventService.getEventDetails(id));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/event/save-event")
    public ResponseEntity<String> saveEvent(@Valid @RequestBody SaveEventRequest request) {
        eventService.saveEvent(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @eventAuthorizer.isOrgManagerOfEvent(#eventId)")
    @PutMapping("/org-manager/event/{eventId}/approve")
    public ResponseEntity<String> approveEventByManager(@PathVariable java.util.UUID eventId) {
        eventService.approveEventByManager(eventId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @eventAuthorizer.isOrgManagerOfEvent(#eventId)")
    @PutMapping("/org-manager/event/{eventId}/reject")
    public ResponseEntity<String> rejectEventByManager(
            @PathVariable java.util.UUID eventId,
            @Valid @RequestBody RejectEventRequest request
    ) {
        eventService.rejectEventByManager(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/event/pending")
    public ResponseEntity<Page<EventSimpleResponseForManager>> getPendingEventsByManager(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String name

    ) {

        return ResponseEntity.ok(eventService.getPendingEventsByManager(pageNumber, pageSize, name));
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/event/approved")
    public ResponseEntity<Page<EventSimpleResponseForManager>> getApprovedEventsByManager(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String name
    ) {
        return ResponseEntity.ok(eventService.getApprovedEventsByManager(pageNumber, pageSize, name));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/event/{eventId}/approve")
    public ResponseEntity<String> approveEventByAdmin(@PathVariable java.util.UUID eventId) {
        eventService.approveEventByAdmin(eventId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/event/{eventId}/reject")
    public ResponseEntity<String> rejectEventByAdmin(
            @PathVariable java.util.UUID eventId,
            @Valid @RequestBody RejectEventRequest request
    ) {
        eventService.rejectEventByAdmin(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/event/pending")
    public ResponseEntity<Page<EventSimpleResponseForAdmin>> getPendingEventsByAdmin(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String name

    ) {

        return ResponseEntity.ok(eventService.getPendingEventsByAdmin(pageNumber, pageSize, name));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/event/running")
    public ResponseEntity<Page<EventSimpleResponseForAdmin>> getRunningEventsByAdmin(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String name
    ) {
        return ResponseEntity.ok(eventService.getRunningEventsByAdmin(pageNumber, pageSize, name));
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/event/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForManager> getEventDetailsByManager(
            @PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId
    ) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(eventService.getEventDetailsByManager(id));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/event/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForSystemAdmin> getEventDetailsBySystemAdmin(
            @PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId
    ) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(eventService.getEventDetailsBySystemAdmin(id));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/event/my-events")
    public ResponseEntity<Page<EventSimpleResponseForHost>> getEventsByHost(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String name,

            @RequestParam(defaultValue = "RECRUITING")
            @EventStatus
            String status
    ) {

        return ResponseEntity.ok(eventService.getEventsByHost(pageNumber, pageSize, name, status));
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#inputId)")
    @GetMapping("/host/event/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForHost> getEventDetailsByHost(
            @PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId
    ) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(eventService.getEventDetailsByHost(id));
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#eventId)")
    @PostMapping("/host/events/{eventId}/announce-volunteers")
    public ResponseEntity<Void> announceVolunteer(
            @PathVariable java.util.UUID eventId,
            @RequestBody @Valid AnnounceVolunteerRequest request
    ) {
        eventService.announceVolunteersOfEvent(eventId, request);
        return ResponseEntity.ok().build();

    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/events/check-event-check-in-code")
    public ResponseEntity<CheckEventCheckInCodeResponse> checkEventCheckInCode(@Valid @RequestBody CheckEventCheckInCodeRequest request) {
        return ResponseEntity.ok(eventService.checkEventCheckInCode(request));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/events/quick-check-in")
    public ResponseEntity<Void> quickCheckIn(@Valid @RequestBody QuickCheckInEventRequest request) {
        eventService.quickCheckInEvent(request);
        return ResponseEntity.ok().build();
    }
}
