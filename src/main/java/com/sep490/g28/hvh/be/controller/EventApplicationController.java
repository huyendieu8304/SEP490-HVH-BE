package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.service.EventApplicationService;
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
}
