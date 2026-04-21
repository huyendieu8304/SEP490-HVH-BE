package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;
import com.sep490.g28.hvh.be.service.EventRatingService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EventRatingController {

    EventRatingService eventRatingService;

    @PreAuthorize("hasRole('VOL') and @eventApplicationAuthorizer.isVolunteerOfEventApplication(#request.eventApplicationId)")
    @PostMapping("/vol/event-ratings")
    public ResponseEntity<Void> rateEvent(
            @RequestBody @Valid RateEventRequest request
    ){
        eventRatingService.rateEvent(request);
        return ResponseEntity.ok().build();
    }
}
