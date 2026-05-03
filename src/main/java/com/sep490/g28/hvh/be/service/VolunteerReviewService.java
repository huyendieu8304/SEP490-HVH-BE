package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.dto.volunteerreview.response.VolunteerReviewResponse;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.entity.VolunteerReview;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface VolunteerReviewService {
    void reviewVolunteer(ReviewVolunteerRequest request);

    VolunteerReview reviewAutomatically(Volunteer volunteer, EventApplication application);

    Page<VolunteerReviewResponse> getReviewsOfVolunteer(UUID volunteerId, int pageSize, int pageNumber);
}
