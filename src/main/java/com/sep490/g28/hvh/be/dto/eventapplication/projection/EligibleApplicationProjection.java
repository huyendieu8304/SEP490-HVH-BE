package com.sep490.g28.hvh.be.dto.eventapplication.projection;

import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.entity.VolunteerReview;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EligibleApplicationProjection {
    private EventApplication application;
    private VolunteerReview review;
    private Volunteer volunteer;

    public EligibleApplicationProjection(
            EventApplication application,
            VolunteerReview review,
            Volunteer volunteer

    ) {
        this.application = application;
        this.review = review;
        this.volunteer = volunteer;
    }
}
