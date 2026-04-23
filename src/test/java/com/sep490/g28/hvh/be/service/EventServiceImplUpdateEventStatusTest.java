package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.projection.EligibleApplicationProjection;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.EventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventServiceImplUpdateEventStatusTest {
    @InjectMocks
    EventServiceImpl service;

    @Mock
    EventRepository eventRepository;

    @Mock
    EventApplicationRepository eventApplicationRepository;

    @Mock
    VolunteerReviewService volunteerReviewService;

    @Mock
    VolunteerReviewRepository volunteerReviewRepository;

    @Mock
    VolunteerRepository volunteerRepository;

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    OrganizationStatsService organizationStatsService;

    @Mock
    CertificateService certificateService;

    @Mock
    NotificationService notificationService;

    private Event event() {

        Organization org = new Organization();
        org.setId(UUID.randomUUID());
        org.setCreditHour(0);

        OrganizationManager mng = new OrganizationManager();
        mng.setId(UUID.randomUUID());
        org.setOrganizationManager(mng);

        Host host = new Host();
        host.setId(UUID.randomUUID());

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setOrganization(org);
        event.setHost(host);
        event.setEndDate(LocalDate.now().minusDays(3));

        EventSession session = new EventSession();
        session.setId(UUID.randomUUID());
        session.setEvent(event);
        session.setApprovedApplicationCount(1);
        session.setStartDateTime(OffsetDateTime.now().minusDays(3).minusHours(6));
        session.setEndDateTime(OffsetDateTime.now().minusDays(3).minusHours(3));

        event.setSessions(List.of(session));

        return event;
    }

    private Event event(EEventStatus status) {
        Event e = new Event();
        e.setId(UUID.randomUUID());
        e.setStatus(status);
        return e;
    }

    // ==== completeEvents ===================================
    @Test
    void completeEvents_fullFlow() {

        Event event = event();
        EventSession session = event.getSessions().get(0);

        Volunteer vol = new Volunteer();
        vol.setCreditScore(0);

        EventApplication app = new EventApplication();
        app.setCreditHour((short)3);

        VolunteerReview review = new VolunteerReview();
        review.setAvgRating((short)5);

        EligibleApplicationProjection projection = mock(EligibleApplicationProjection.class);

        when(projection.getVolunteer()).thenReturn(vol);
        when(projection.getApplication()).thenReturn(app);
        when(projection.getReview()).thenReturn(null, review);

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of(event));

        when(eventApplicationRepository.findEligibleApplicationProjection(session.getId()))
                .thenReturn(List.of(projection));

        when(volunteerReviewService.reviewAutomatically(vol, app))
                .thenReturn(review);


        service.completeEvents();

        verify(volunteerReviewService).reviewAutomatically(vol, app);
        verify(volunteerReviewRepository).saveAll(any());
        verify(volunteerRepository).saveAll(any());

        verify(certificateService).generateCertificates(anyList(), eq(event));
        verify(notificationService).sendVolunteersReceivedCertificatesNotifications(anyList(), eq(event));

        assertEquals(EEventStatus.COMPLETED, event.getStatus());
    }

    @Test
    void completeEvents_existingReview_noAutoReview() {

        Event event = event();
        EventSession session = event.getSessions().get(0);

        Volunteer vol = new Volunteer();

        EventApplication app = new EventApplication();
        app.setCreditHour((short)3);

        VolunteerReview review = new VolunteerReview();
        review.setAvgRating((short)5);

        EligibleApplicationProjection projection = mock(EligibleApplicationProjection.class);

        when(projection.getVolunteer()).thenReturn(vol);
        when(projection.getApplication()).thenReturn(app);
        when(projection.getReview()).thenReturn(review);

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of(event));

        when(eventApplicationRepository.findEligibleApplicationProjection(session.getId()))
                .thenReturn(List.of(projection));

        service.completeEvents();

        verify(volunteerReviewService, never()).reviewAutomatically(any(), any());
    }

    @Test
    void completeEvents_notEnoughCredit_noCertificate() {

        Event event = event();
        EventSession session = event.getSessions().get(0);

        Volunteer vol = new Volunteer();

        EventApplication app = new EventApplication();
        app.setCreditHour((short)1); // thấp

        VolunteerReview review = new VolunteerReview();
        review.setAvgRating((short)1);

        EligibleApplicationProjection projection = mock(EligibleApplicationProjection.class);

        when(projection.getVolunteer()).thenReturn(vol);
        when(projection.getApplication()).thenReturn(app);
        when(projection.getReview()).thenReturn(review);

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of(event));

        when(eventApplicationRepository.findEligibleApplicationProjection(session.getId()))
                .thenReturn(List.of(projection));

        service.completeEvents();

        verify(certificateService).generateCertificates(eq(List.of()), eq(event));
    }

    @Test
    void completeEvents_noEvent() {

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of());

        service.completeEvents();

        verifyNoInteractions(volunteerRepository);
        verifyNoInteractions(certificateService);
    }

    @Test
    void completeEvents_noEligibleApplications() {

        Event event = event();
        EventSession session = event.getSessions().get(0);

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of(event));

        when(eventApplicationRepository.findEligibleApplicationProjection(session.getId()))
                .thenReturn(List.of());

        service.completeEvents();

        verify(volunteerReviewRepository).saveAll(any());
    }

    @Test
    void completeEvents_updateOrganizationAndStats() {

        Event event = event();

        when(eventRepository.findEndedEventsAndEndDateBefore(any()))
                .thenReturn(List.of(event));

        when(eventApplicationRepository.findEligibleApplicationProjection(any()))
                .thenReturn(List.of());

        service.completeEvents();

        verify(organizationRepository).save(event.getOrganization());

        verify(organizationStatsService)
                .updateOrganizationCreditHoursAndCountApplicationsAndCountCompletedEventStats(
                        any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()
                );

        verify(eventRepository).save(event);
    }

    // ==== deleteEvent ===================================
    @Test
    void deleteEvent_success_shouldDelete() {
        Event e = event(EEventStatus.EDITING);

        when(eventRepository.findById(e.getId()))
                .thenReturn(Optional.of(e));

        service.deleteEvent(e.getId());

        verify(eventRepository).delete(e);
    }

    @Test
    void deleteEvent_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();

        when(eventRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.deleteEvent(id));
    }

    @Test
    void deleteEvent_invalidStatus_shouldThrow() {
        Event e = event(EEventStatus.ONGOING);

        when(eventRepository.findById(e.getId()))
                .thenReturn(Optional.of(e));

        assertThrows(AppException.class,
                () -> service.deleteEvent(e.getId()));
    }

    // ================= endRecruitment =================

    @Test
    void endRecruitment_shouldUpdateStatus() {
        Event e = event(EEventStatus.RECRUITING);

        when(eventRepository.findRecruitingEventsAndRecruitmentEndDateBefore(any()))
                .thenReturn(List.of(e));

        service.endRecruitment();

        assertEquals(EEventStatus.UPCOMING, e.getStatus());

        verify(eventRepository).save(e);
        verify(eventRepository).saveAll(List.of(e));
    }

    @Test
    void endRecruitment_empty_shouldNotCrash() {
        when(eventRepository.findRecruitingEventsAndRecruitmentEndDateBefore(any()))
                .thenReturn(List.of());

        service.endRecruitment();

        verify(eventRepository).saveAll(List.of());
    }

    // ================= startEvents =================

    @Test
    void startEvents_shouldUpdateStatus() {
        Event e = event(EEventStatus.UPCOMING);

        when(eventRepository.findUpcomingEventsAndStartDateToday(any()))
                .thenReturn(List.of(e));

        service.startEvents();

        assertEquals(EEventStatus.ONGOING, e.getStatus());

        verify(eventRepository).save(e);
        verify(eventRepository).saveAll(List.of(e));
    }

    @Test
    void startEvents_empty_shouldNotCrash() {
        when(eventRepository.findUpcomingEventsAndStartDateToday(any()))
                .thenReturn(List.of());

        service.startEvents();

        verify(eventRepository).saveAll(List.of());
    }

    // ================= endEvents =================

    @Test
    void endEvents_shouldUpdateStatus() {
        Event e = event(EEventStatus.ONGOING);

        when(eventRepository.findOngoingEventsAndEndDateYesterday(any()))
                .thenReturn(List.of(e));

        service.endEvents();

        assertEquals(EEventStatus.ENDED, e.getStatus());

        verify(eventRepository).save(e);
        verify(eventRepository).saveAll(List.of(e));
    }

    @Test
    void endEvents_empty_shouldNotCrash() {
        when(eventRepository.findOngoingEventsAndEndDateYesterday(any()))
                .thenReturn(List.of());

        service.endEvents();

        verify(eventRepository).saveAll(List.of());
    }
}
