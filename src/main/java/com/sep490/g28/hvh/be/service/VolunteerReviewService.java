package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.entity.VolunteerReview;

public interface VolunteerReviewService {
    void reviewVolunteer(ReviewVolunteerRequest request);

    VolunteerReview reviewAutomatically(Volunteer volunteer, EventApplication application);
}
