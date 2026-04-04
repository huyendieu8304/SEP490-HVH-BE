package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.service.VolunteerReviewService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class VolunteerReviewController {

    VolunteerReviewService volunteerReviewService;

    @PreAuthorize("hasRole('HOST') and @eventApplicationAuthorizer.isHostOfEventApplication(#request.eventApplicationId)")
    @PostMapping("/volunteer-reviews")
    public ResponseEntity<Void> rateEvent(
            @RequestBody @Valid ReviewVolunteerRequest request
    ){
        volunteerReviewService.reviewVolunteer(request);
        return ResponseEntity.ok().build();
    }
}
