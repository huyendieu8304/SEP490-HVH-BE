package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.event.request.*;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.service.EventService;
import com.sep490.g28.hvh.be.validation.EventStatus;
import com.sep490.g28.hvh.be.validation.ValidLatitude;
import com.sep490.g28.hvh.be.validation.ValidLongitude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventController {

    EventService eventService;

    @GetMapping("/events/feeds")
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
            List<Short> activitySubDomainIds,

            @RequestParam(required = false)
            @ValidLatitude
            Double currentPlaceLat,

            @RequestParam(required = false)
            @ValidLongitude
            Double currentPlaceLng,

            @RequestParam(required = false)
            Double distance
    ) {
        return ResponseEntity.ok(eventService.getEventFeeds(
                pageNumber,
                pageSize,
                refresh,
                name,
                address,
                startDate,
                endDate,
                activitySubDomainIds,
                currentPlaceLat,
                currentPlaceLng,
                distance));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/events/draft")
    ResponseEntity<EditEventResponse> draftEvent(@RequestBody @Valid EditEventRequest request) {

        return ResponseEntity.ok(eventService.draftEvent(request));
    }

    @PreAuthorize("hasRole('HOST')")
    @PostMapping("/host/events/submit")
    ResponseEntity<EditEventResponse> submitEvent(@RequestBody @Valid EditEventRequest request) {

        return ResponseEntity.ok(eventService.submitEvent(request));
    }

    @GetMapping("/events/event-details/{id}")
    public ResponseEntity<EventDetailsResponse> getEventDetails(
            @PathVariable(name = "id") UUID id
    ) {
        return ResponseEntity.ok(eventService.getEventDetails(id));
    }

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/events/save-event")
    public ResponseEntity<String> saveEvent(@Valid @RequestBody SaveEventRequest request) {
        eventService.saveEvent(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @eventAuthorizer.isOrgManagerOfEvent(#eventId)")
    @PutMapping("/org-manager/events/{eventId}/approve")
    public ResponseEntity<String> approveEventByManager(@PathVariable UUID eventId) {
        eventService.approveEventByManager(eventId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @eventAuthorizer.isOrgManagerOfEvent(#eventId)")
    @PutMapping("/org-manager/events/{eventId}/reject")
    public ResponseEntity<String> rejectEventByManager(
            @PathVariable UUID eventId,
            @Valid @RequestBody RejectEventRequest request
    ) {
        eventService.rejectEventByManager(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/events/pending")
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
    @GetMapping("/org-manager/events/approved")
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
    @PutMapping("/sys-admin/events/{eventId}/approve")
    public ResponseEntity<String> approveEventByAdmin(@PathVariable UUID eventId) {
        eventService.approveEventByAdmin(eventId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/events/{eventId}/reject")
    public ResponseEntity<String> rejectEventByAdmin(
            @PathVariable UUID eventId,
            @Valid @RequestBody RejectEventRequest request
    ) {
        eventService.rejectEventByAdmin(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/events/pending")
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
    @GetMapping("/sys-admin/events/running")
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
    @GetMapping("/org-manager/events/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForManager> getEventDetailsByManager(
            @PathVariable(name = "id") UUID id
    ) {
        return ResponseEntity.ok(eventService.getEventDetailsByManager(id));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/events/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForSystemAdmin> getEventDetailsBySystemAdmin(
            @PathVariable(name = "id") UUID id
    ) {
        return ResponseEntity.ok(eventService.getEventDetailsBySystemAdmin(id));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/events/my-events")
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
    @GetMapping("/host/events/event-details/{id}")
    public ResponseEntity<EventDetailsResponseForHost> getEventDetailsByHost(
            @PathVariable(name = "id") UUID inputId
    ) {
        return ResponseEntity.ok(eventService.getEventDetailsByHost(inputId));
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#eventId)")
    @PostMapping("/host/events/{eventId}/announce-volunteers")
    public ResponseEntity<Void> announceVolunteer(
            @PathVariable UUID eventId,
            @RequestBody @Valid AnnounceVolunteerRequest request
    ) {
        eventService.announceVolunteersOfEvent(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#eventId)")
    @PutMapping("/host/events/{eventId}/cancel")
    public ResponseEntity<Void> cancelEventByHost(
            @PathVariable UUID eventId,
            @RequestBody @Valid CancelEventRequest request
    ) {
        eventService.cancelEventByHost(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/events/{eventId}/cancel")
    public ResponseEntity<Void> cancelEventByAdmin(
            @PathVariable UUID eventId,
            @RequestBody @Valid CancelEventRequest request
    ) {
        eventService.cancelEventByAdmin(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#eventId)")
    @PutMapping("/host/events/{eventId}/update")
    public ResponseEntity<UpdateEventResponse> updateEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request
    ){
        return ResponseEntity.ok(eventService.updateEvent(eventId, request));
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @eventAuthorizer.isOrgManagerOfEvent(#eventId)")
    @PutMapping("/org-manager/events/{eventId}/assign-host")
    public ResponseEntity<Void> assignHostToEvent(
            @PathVariable UUID eventId,
            @Valid @RequestBody AssignHostToEventRequest request
    ){
        eventService.assignHostToEvent(eventId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST') and @eventAuthorizer.isHostOfEvent(#eventId)")
    @DeleteMapping("/host/events/{eventId}")
    public ResponseEntity<UpdateEventResponse> deleteEvent(
            @PathVariable UUID eventId
    ){
        eventService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('VOL')")
    @GetMapping("/vol/events/saved-events")
    public ResponseEntity<Page<EventSimpleResponse>> getSavedEventsByVolunteer(
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

        return ResponseEntity.ok(eventService.getSavedEventsByVolunteer(pageNumber, pageSize, name));
    }

    @GetMapping("/events/{organizationId}/running")
    public ResponseEntity<Page<EventSimpleResponse>> getRunningEvents(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @PathVariable(name = "organizationId")
            UUID orgId,

            @RequestParam(required = false)
            String name
    ) {
        return ResponseEntity.ok(eventService.getHostedEventsOfOrganization(pageNumber, pageSize, orgId, name));
    }

    @PreAuthorize("hasRole('VOL')")
    @DeleteMapping("/vol/events/un-save-event")
    public ResponseEntity<Void> unSaveEvent(@Valid @RequestBody UnSaveEventRequest request) {
        eventService.unSaveEvent(request);
        return ResponseEntity.ok().build();
    }
}
