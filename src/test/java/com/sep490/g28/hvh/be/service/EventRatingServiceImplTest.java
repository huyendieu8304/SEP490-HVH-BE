package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventRating;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.RateAndReviewErrorCode;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventRatingRepository;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.service.impl.EventRatingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventRatingServiceImplTest {

    @InjectMocks
    EventRatingServiceImpl service;

    @Mock
    EventRepository eventRepository;
    @Mock
    EventApplicationRepository eventApplicationRepository;
    @Mock
    EventRatingRepository eventRatingRepository;

    private Event mockEventForRating(){
        Event event = new Event();
        event = new Event();
        event.setId(UUID.randomUUID());
        event.setAvgRating((short) 4);
        event.setRatingCount(2L);
        event.setEndDate(LocalDate.now().minusDays(1));

        EventSession session = new EventSession();
        session.setEvent(event);

        event.setSessions(List.of(session));
        return event;
    }

    private RateEventRequest mockRateEventRequest(){
        RateEventRequest request = new RateEventRequest();
        request.setEventApplicationId(UUID.randomUUID());
        request.setOrganizationQualityRating((short) 4);
        request.setProfessionalismRating((short) 4);
        request.setWorkEnvironmentRating((short) 4);
        request.setValueImpactRating((short) 4);
        request.setSupportConnectionRating((short) 4);
        return request;
    }

    private EventApplication mockEventApplication(EventSession session, RateEventRequest request){
        EventApplication application = new EventApplication();
        application = new EventApplication();
        application.setId(request.getEventApplicationId());
        application.setSession(session);
        application.setStatus(EEventApplicationStatus.COMPLETED);
        return application;
    }

    // ===== rateEvent
    @Test
    void rateEvent_applicationNotFound_shouldThrow() {
        RateEventRequest request = mockRateEventRequest();

        when(eventApplicationRepository.findById(request.getEventApplicationId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.rateEvent(request));
        assertThat(ex.getCode()).isEqualTo(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED.getCode());
    }

    @Test
    void rateEvent_notCompleted_shouldThrow() {
        RateEventRequest request = mockRateEventRequest();

        Event event = mockEventForRating();
        EventSession session = event.getSessions().get(0);

        EventApplication app = mockEventApplication(session, request);
        app.setStatus(EEventApplicationStatus.PENDING); // khác COMPLETED

        when(eventApplicationRepository.findById(request.getEventApplicationId()))
                .thenReturn(Optional.of(app));

        AppException ex = assertThrows(AppException.class,
                () -> service.rateEvent(request));
        assertThat(ex.getCode()).isEqualTo(RateAndReviewErrorCode.NOT_RECORDED_AS_PARTICIPANT.getCode());
    }

    @Test
    void rateEvent_alreadyRated_shouldThrow() {
        RateEventRequest request = mockRateEventRequest();

        Event event = mockEventForRating();
        EventSession session = event.getSessions().get(0);
        EventApplication app = mockEventApplication(session, request);

        when(eventApplicationRepository.findById(request.getEventApplicationId()))
                .thenReturn(Optional.of(app));

        when(eventRatingRepository.findByEventApplication_Id(request.getEventApplicationId()))
                .thenReturn(Optional.of(new EventRating()));

        AppException ex = assertThrows(AppException.class,
                () -> service.rateEvent(request));
        assertThat(ex.getCode()).isEqualTo(RateAndReviewErrorCode.ALREADY_RATED_EVENT.getCode());
    }

    @Test
    void rateEvent_outOfAllowedTime_shouldThrow() {
        RateEventRequest request = mockRateEventRequest();

        Event event = mockEventForRating();
        EventSession session = event.getSessions().get(0);

        EventApplication app = mockEventApplication(session, request);
        app.setSessionDate(LocalDate.now().minusDays(8)); // quá 7 ngày

        when(eventApplicationRepository.findById(request.getEventApplicationId()))
                .thenReturn(Optional.of(app));

        when(eventRatingRepository.findByEventApplication_Id(request.getEventApplicationId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> service.rateEvent(request));
        assertThat(ex.getCode()).isEqualTo(RateAndReviewErrorCode.RATE_EVENT_NOT_IN_ALLOWED_TIME.getCode());
    }

    @Test
    void rateEvent_valid_shouldSaveRatingAndUpdateEvent() {
        RateEventRequest request = mockRateEventRequest();

        Event event = mockEventForRating(); // avg=4, count=2
        EventSession session = event.getSessions().get(0);

        EventApplication app = mockEventApplication(session, request);
        app.setSessionDate(LocalDate.now());

        when(eventApplicationRepository.findById(request.getEventApplicationId()))
                .thenReturn(Optional.of(app));

        when(eventRatingRepository.findByEventApplication_Id(request.getEventApplicationId()))
                .thenReturn(Optional.empty());

        // mock save rating (rating avg = 4)
        EventRating savedRating = new EventRating();
        savedRating.setOrganizationQualityRating((short)4);
        savedRating.setProfessionalismRating((short)4);
        savedRating.setWorkEnvironmentRating((short)4);
        savedRating.setValueImpactRating((short)4);
        savedRating.setSupportConnectionRating((short)4);
        savedRating.setAvgRating((short)4);

        when(eventRatingRepository.save(any())).thenReturn(savedRating);

        service.rateEvent(request);

        // verify save rating
        verify(eventRatingRepository).save(any());

        // verify event updated
        verify(eventRepository).save(event);

        // optional: verify logic tính toán
        // newAvg = (4*2 + 4) / 3 = 4
        assertThat(event.getAvgRating()).isEqualTo((short) 4);
        assertThat(event.getRatingCount()).isEqualTo(3);
    }
}
