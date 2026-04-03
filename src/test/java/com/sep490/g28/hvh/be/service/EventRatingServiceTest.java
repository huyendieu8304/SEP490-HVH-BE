package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventRating;
import com.sep490.g28.hvh.be.entity.EventSession;
import com.sep490.g28.hvh.be.exception.AppException;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventRatingServiceTest {

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
    void rateEvent_application_not_found_should_throw() {
        RateEventRequest request = mockRateEventRequest();
        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.rateEvent(request));
    }

    @Test
    void rateEvent_not_completed_should_throw() {
        RateEventRequest request = mockRateEventRequest();
        EventSession session = new EventSession();
        EventApplication application = mockEventApplication(session, request);
        application.setStatus(EEventApplicationStatus.PENDING);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        assertThrows(AppException.class,
                () -> service.rateEvent(request));
    }

    @Test
    void rateEvent_already_rated_should_throw() {
        RateEventRequest request = mockRateEventRequest();
        EventSession session = new EventSession();
        EventApplication application = mockEventApplication(session, request);
        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        when(eventRatingRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.of(new EventRating()));

        assertThrows(AppException.class,
                () -> service.rateEvent(request));
    }

    @Test
    void rateEvent_before_event_end_should_throw() {
        Event event = mockEventForRating();
        RateEventRequest request = mockRateEventRequest();
        EventApplication application = mockEventApplication(event.getSessions().get(0), request);
        event.setEndDate(LocalDate.now().plusDays(1));

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        when(eventRatingRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.rateEvent(request));
    }

    @Test
    void rateEvent_after_7_days_should_throw() {
        Event event = mockEventForRating();
        RateEventRequest request = mockRateEventRequest();
        EventApplication application = mockEventApplication(event.getSessions().get(0), request);

        event.setEndDate(LocalDate.now().minusDays(10));

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        when(eventRatingRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.rateEvent(request));
    }

    @Test
    void rateEvent_success() {
        Event event = mockEventForRating();
        RateEventRequest request = mockRateEventRequest();
        EventApplication application = mockEventApplication(event.getSessions().get(0), request);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        when(eventRatingRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        when(eventRatingRepository.save(any()))
                .thenAnswer(inv -> {
                    EventRating r = inv.getArgument(0);
                    // giả lập avgRating của rating
                    r.setAvgRating((short) 4);
                    return r;
                });

        service.rateEvent(request);

        // verify save rating
        verify(eventRatingRepository).save(argThat(r ->
                r.getEventApplication() == application &&
                        r.getOrganizationQualityRating() == 4
        ));

        // verify update event
        verify(eventRepository).save(argThat(e -> {
            // old: avg=4, count=2
            // new: (4*2 + 4) / 3 = 4
            return e.getRatingCount() == 3 &&
                    e.getAvgRating() == 4;
        }));
    }

    @Test
    void rateEvent_recalculate_avg_correctly() {
        Event event = mockEventForRating();
        RateEventRequest request = mockRateEventRequest();
        EventApplication application = mockEventApplication(event.getSessions().get(0), request);
        event.setAvgRating((short) 5);
        event.setRatingCount(1L);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(application));

        when(eventRatingRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        // rating mới = 3
        when(eventRatingRepository.save(any()))
                .thenAnswer(inv -> {
                    EventRating r = inv.getArgument(0);
                    r.setAvgRating((short) 3);
                    return r;
                });

        service.rateEvent(request);

        verify(eventRepository).save(argThat(e ->
                e.getRatingCount() == 2 &&
                        e.getAvgRating() == 4   // (5*1 + 3)/2 = 4
        ));
    }
}
