package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventapplication.RejectApplicationRequest;
import com.sep490.g28.hvh.be.service.EventApplicationService;
import jakarta.validation.Valid;
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
    @PutMapping("/host/event-applications/{id}/approve")
    ResponseEntity<Void> approveApplication(@PathVariable UUID id) {
        eventApplicationService.approveApplication(id);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('HOST')")
    @PutMapping("/host/event-applications/{id}/reject")
    ResponseEntity<Void> rejectApplication(
            @PathVariable UUID id,
            @RequestBody @Valid RejectApplicationRequest request
    ) {
        eventApplicationService.rejectApplication(id, request);
        return ResponseEntity.ok().build();
    }
}
