package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.service.EventApplicationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
}
