package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.ShareMomentResponse;
import com.sep490.g28.hvh.be.service.EventMomentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventMomentController {

    EventMomentService eventMomentService;

    @PreAuthorize("hasRole('VOL')")
    @PostMapping("/vol/event-moments")
    public ResponseEntity<ShareMomentResponse> shareMoment(@RequestBody @Valid ShareMomentRequest request) {

        return ResponseEntity.ok(eventMomentService.shareMoment(request));
    }

    @GetMapping("/event-moments/feed")
    public ResponseEntity<EventMomentFeedResponse> getEventMomentsFeed(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String eventName
    ) {
        return ResponseEntity.ok(eventMomentService.getEventMomentsFeed(pageNumber, pageSize, eventName));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @DeleteMapping("/sys-admin/event-moments/{momentId}/delete-moment")
    public ResponseEntity<Void> deleteEventMoment(
            @PathVariable(name = "momentId") @UUID(message = "INVALID_UUID") String inputId) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        eventMomentService.deleteEventMoment(id);
        return ResponseEntity.ok().build();
    }
}
