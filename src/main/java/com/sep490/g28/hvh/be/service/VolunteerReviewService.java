package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import jakarta.validation.Valid;

public interface VolunteerReviewService {
    void reviewVolunteer(ReviewVolunteerRequest request);
}
