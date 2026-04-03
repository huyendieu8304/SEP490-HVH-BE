package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.entity.VolunteerReview;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.RateAndReviewErrorCode;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.repository.VolunteerReviewRepository;
import com.sep490.g28.hvh.be.service.NotificationService;
import com.sep490.g28.hvh.be.service.VolunteerReviewService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VolunteerReviewImpl implements VolunteerReviewService {

    EventApplicationRepository eventApplicationRepository;
    VolunteerReviewRepository volunteerReviewRepository;

    NotificationService notificationService;
    VolunteerRepository volunteerRepository;

    @Override
    @Transactional
    public void reviewVolunteer(ReviewVolunteerRequest request) {
        //check participate of volunteer
        EventApplication application = eventApplicationRepository.findById(request.getEventApplicationId()).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED)
        );

        if (application.getStatus() != EEventApplicationStatus.COMPLETED){
            throw new AppException(RateAndReviewErrorCode.NOT_RECORDED_AS_PARTICIPANT);
        }

        //make sure only one review for a volunteer in a session
        if (volunteerReviewRepository.findByEventApplication_Id(request.getEventApplicationId()).isPresent()){
            throw new AppException(RateAndReviewErrorCode.ALREADY_REVIEWED_VOL);
        }

        //check event status (whether review vol is allowed)
        Event event = application.getSession().getEvent();
        if (event.getStatus() != EEventStatus.ENDED){
            throw new AppException(RateAndReviewErrorCode.REVIEW_VOL_NOT_IN_ALLOWED_TIME);
        }

        //create review record
        VolunteerReview review = new VolunteerReview();
        review.setEventApplication(application);
        review.setProfessionalAttitudeRating(request.getProfessionalAttitudeRating());
        review.setResponsibilityPunctualityRating(request.getResponsibilityPunctualityRating());
        review.setWorkEffectivenessRating(request.getWorkEffectivenessRating());
        review.setTeamworkCommunicationRating(request.getTeamworkCommunicationRating());
        review.setAdaptabilityProblemSolvingRating(request.getAdaptabilityProblemSolvingRating());
        review.setComment(request.getComment());

        review = volunteerReviewRepository.save(review);

        //re calculate the avg rating and increase rating count of volunteer
        Volunteer volunteer = application.getVolunteer();
        short newAvg = (short) ((volunteer.getAvgRating() * volunteer.getRatingCount() + review.getAvgRating()) / (volunteer.getRatingCount() + 1));
        long newCount = volunteer.getRatingCount() + 1;

        volunteer.setAvgRating(newAvg);
        volunteer.setRatingCount(newCount);
        volunteerRepository.save(volunteer);

        //send notification to vol
        notificationService.sendVolunteerReviewedByHostNotification(volunteer.getId(), event, application, review.getId());
        log.info("Volunteer is reviewed, volunteerId={}, applicationId={}",
                volunteer.getId(),
                application.getId()
        );
    }
}
