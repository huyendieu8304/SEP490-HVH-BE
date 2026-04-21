package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventRating;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.RateAndReviewErrorCode;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventRatingRepository;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.service.EventRatingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventRatingServiceImpl implements EventRatingService {

    EventRepository eventRepository;
    EventApplicationRepository eventApplicationRepository;
    EventRatingRepository eventRatingRepository;

    @Override
    @Transactional
    public void rateEvent(RateEventRequest request) {
        //check vol participate
        EventApplication application = eventApplicationRepository.findById(request.getEventApplicationId()).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED)
        );

        if (application.getStatus() != EEventApplicationStatus.COMPLETED){
            throw new AppException(RateAndReviewErrorCode.NOT_RECORDED_AS_PARTICIPANT);
        }

        //make sure only one rating for a session
        if (eventRatingRepository.findByEventApplication_Id(request.getEventApplicationId()).isPresent()){
            throw new AppException(RateAndReviewErrorCode.ALREADY_RATED_EVENT);
        }

        //get event
        Event event = application.getSession().getEvent();

        LocalDate now = LocalDate.now();
        //check rating time, in 7 days after the event session date in the application
        if ( now.isAfter(application.getSessionDate().plusDays(7))){
            throw new AppException(RateAndReviewErrorCode.RATE_EVENT_NOT_IN_ALLOWED_TIME);
        }

        //create rating record
        EventRating rating = new EventRating();
        rating.setEventApplication(application);
        rating.setOrganizationQualityRating(request.getOrganizationQualityRating());
        rating.setProfessionalismRating(request.getProfessionalismRating());
        rating.setWorkEnvironmentRating(request.getWorkEnvironmentRating());
        rating.setValueImpactRating(request.getValueImpactRating());
        rating.setSupportConnectionRating(request.getSupportConnectionRating());

        rating = eventRatingRepository.save(rating);

        //re calculate the avg rating and increase rating count of event
        short newAvg = (short) ((event.getAvgRating() * event.getRatingCount() + rating.getAvgRating()) / (event.getRatingCount() + 1));
        long newCount = event.getRatingCount() + 1;

        event.setAvgRating(newAvg);
        event.setRatingCount(newCount);
        eventRepository.save(event);
        log.info("Event is rated, eventId={}, applicationId={}, avgRating={}",
                event.getId(),
                application.getId(),
                rating.getAvgRating()
        );
    }
}
