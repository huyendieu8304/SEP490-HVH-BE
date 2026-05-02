package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.dto.volunteerreview.response.VolunteerReviewResponse;
import com.sep490.g28.hvh.be.service.VolunteerReviewService;
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
public class VolunteerReviewController {

    VolunteerReviewService volunteerReviewService;

    @PreAuthorize("hasRole('HOST') and @eventApplicationAuthorizer.isHostOfEventApplication(#request.eventApplicationId)")
    @PostMapping("/host/volunteer-reviews")
    public ResponseEntity<Void> reviewVolunteer(
            @RequestBody @Valid ReviewVolunteerRequest request
    ){
        volunteerReviewService.reviewVolunteer(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/volunteer-reviews/{volunteerId}")
    public ResponseEntity<Page<VolunteerReviewResponse>> getReviewsOfVolunteer(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @PathVariable UUID volunteerId
    ){
        return ResponseEntity.ok(volunteerReviewService.getReviewsOfVolunteer(volunteerId, pageSize, pageNumber));
    }
}
