package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.volunteerreview.request.ReviewVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.repository.VolunteerReviewRepository;
import com.sep490.g28.hvh.be.service.impl.VolunteerReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VolunteerReviewServiceTest {

    @Mock
    EventApplicationRepository eventApplicationRepository;
    @Mock
    VolunteerReviewRepository volunteerReviewRepository;

    @Mock
    NotificationService notificationService;
    @Mock
    VolunteerRepository volunteerRepository;

    @InjectMocks
    VolunteerReviewServiceImpl volunteerReviewService;

    private ReviewVolunteerRequest mockRequest() {
        ReviewVolunteerRequest request = new ReviewVolunteerRequest();
        request.setEventApplicationId(UUID.randomUUID());
        request.setProfessionalAttitudeRating((short) 4);
        request.setResponsibilityPunctualityRating((short) 4);
        request.setWorkEffectivenessRating((short) 4);
        request.setTeamworkCommunicationRating((short) 4);
        request.setAdaptabilityProblemSolvingRating((short) 4);
        request.setComment("good");
        return request;
    }

    private EventApplication mockApplication(ReviewVolunteerRequest request) {
        EventApplication app = new EventApplication();
        app.setId(request.getEventApplicationId());
        app.setStatus(EEventApplicationStatus.COMPLETED);

        Event event = new Event();
        event.setStatus(EEventStatus.ENDED);

        EventSession session = new EventSession();
        session.setEvent(event);

        Volunteer volunteer = new Volunteer();
        volunteer.setId(UUID.randomUUID());
        volunteer.setAvgRating((short) 4);
        volunteer.setRatingCount(2L);

        app.setSession(session);
        app.setVolunteer(volunteer);

        return app;
    }

    // ===== reviewVolunteer

    @Test
    void reviewVolunteer_application_not_found_should_throw() {
        ReviewVolunteerRequest request = mockRequest();

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerReviewService.reviewVolunteer(request));
    }

    @Test
    void reviewVolunteer_not_completed_should_throw() {
        ReviewVolunteerRequest request = mockRequest();
        EventApplication app = mockApplication(request);
        app.setStatus(EEventApplicationStatus.PENDING);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(app));

        assertThrows(AppException.class,
                () -> volunteerReviewService.reviewVolunteer(request));
    }

    @Test
    void reviewVolunteer_already_reviewed_should_throw() {
        ReviewVolunteerRequest request = mockRequest();
        EventApplication app = mockApplication(request);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(app));

        when(volunteerReviewRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.of(new VolunteerReview()));

        assertThrows(AppException.class,
                () -> volunteerReviewService.reviewVolunteer(request));
    }

    @Test
    void reviewVolunteer_event_not_ended_should_throw() {
        ReviewVolunteerRequest request = mockRequest();
        EventApplication app = mockApplication(request);
        app.getSession().getEvent().setStatus(EEventStatus.ONGOING);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(app));

        when(volunteerReviewRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerReviewService.reviewVolunteer(request));
    }

    @Test
    void reviewVolunteer_success() {
        ReviewVolunteerRequest request = mockRequest();
        EventApplication app = mockApplication(request);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(app));

        when(volunteerReviewRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        when(volunteerReviewRepository.save(any()))
                .thenAnswer(inv -> {
                    VolunteerReview r = inv.getArgument(0);
                    r.setId(UUID.randomUUID());
                    r.setAvgRating((short) 4);
                    return r;
                });

        volunteerReviewService.reviewVolunteer(request);

        // verify save review
        verify(volunteerReviewRepository).save(argThat(r ->
                r.getEventApplication() == app &&
                        r.getProfessionalAttitudeRating() == 4
        ));

        // verify update volunteer
        verify(volunteerRepository).save(argThat(v -> {
            // old: avg=4, count=2
            // new: (4*2 + 4)/3 = 4
            return v.getRatingCount() == 3 &&
                    v.getAvgRating() == 4;
        }));

        // verify notification
        verify(notificationService).sendVolunteerReviewedByHostNotification(
                eq(app.getVolunteer().getId()),
                eq(app.getSession().getEvent()),
                eq(app),
                any()
        );
    }

    @Test
    void reviewVolunteer_recalculate_avg_correctly() {
        ReviewVolunteerRequest request = mockRequest();
        EventApplication app = mockApplication(request);

        app.getVolunteer().setAvgRating((short) 5);
        app.getVolunteer().setRatingCount(1L);

        when(eventApplicationRepository.findById(any()))
                .thenReturn(Optional.of(app));

        when(volunteerReviewRepository.findByEventApplication_Id(any()))
                .thenReturn(Optional.empty());

        when(volunteerReviewRepository.save(any()))
                .thenAnswer(inv -> {
                    VolunteerReview r = inv.getArgument(0);
                    r.setAvgRating((short) 3);
                    return r;
                });

        volunteerReviewService.reviewVolunteer(request);

        verify(volunteerRepository).save(argThat(v ->
                v.getRatingCount() == 2 &&
                        v.getAvgRating() == 4   // (5*1 + 3)/2 = 4
        ));
    }
}
