package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.event.request.*;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

import java.util.UUID;

public interface EventService {

    EventFeedResponse getEventFeeds(int pageNumber,
                                    int pageSize,
                                    boolean refresh,
                                    String name,
                                    String address,
                                    LocalDate startDate,
                                    LocalDate endDate,
                                    List<Short> activitySubDomains);

    EditEventResponse draftEvent(EditEventRequest request);

    EditEventResponse submitEvent(EditEventRequest request);

    EventDetailsResponse getEventDetails(UUID id);

    void saveEvent(SaveEventRequest saveEventRequest);

    void approveEventByManager(UUID eventId);

    void rejectEventByManager(UUID eventId, RejectEventRequest request);

    void approveEventByAdmin(UUID eventId);

    void rejectEventByAdmin(UUID eventId, RejectEventRequest request);

    Page<EventSimpleResponseForManager> getPendingEventsByManager(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForManager> getApprovedEventsByManager(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForAdmin> getPendingEventsByAdmin(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForAdmin> getRunningEventsByAdmin(int pageNumber, int pageSize, String eventName);

    EventDetailsResponseForManager getEventDetailsByManager(UUID eventId);

    EventDetailsResponseForSystemAdmin getEventDetailsBySystemAdmin(UUID eventId);

    Page<EventSimpleResponseForHost> getEventsByHost(int pageNumber, int pageSize, String eventName, String status);

    EventDetailsResponseForHost getEventDetailsByHost(UUID eventId);

    void announceVolunteersOfEvent(UUID eventId, AnnounceVolunteerRequest request);

    void cancelEventByHost(UUID eventId, CancelEventRequest request);

    void cancelEventByAdmin(UUID eventId, CancelEventRequest request);

    UpdateEventResponse updateEvent(UUID eventId, UpdateEventRequest request);

    void assignHostToEvent(UUID eventId, AssignHostToEventRequest request);

    void completeEvents();

    void deleteEvent(UUID eventId);

    void endRecruitment();

    void startEvents();

    void endEvents();

    Page<EventSimpleResponse> getSavedEventsByVolunteer(int pageNumber, int pageSize, String inputName);

    Page<EventSimpleResponse> getHostedEventsOfOrganization(int pageNumber, int pageSize, UUID organizationId, String eventName);

    void unSaveEvent(UnSaveEventRequest request);
}
