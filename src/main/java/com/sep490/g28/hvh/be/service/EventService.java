package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.event.request.RejectEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.SaveEventRequest;
import com.sep490.g28.hvh.be.dto.event.response.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

import com.sep490.g28.hvh.be.dto.event.request.EditEventRequest;
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

    Page<EventSimpleResponseForManager> getPendingEventsForManager(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForManager> getApprovedEventsForManager(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForAdmin> getPendingEventsForAdmin(int pageNumber, int pageSize, String eventName);

    Page<EventSimpleResponseForAdmin> getRunningEventsForAdmin(int pageNumber, int pageSize, String eventName);

    EventDetailsResponseForManager getEventDetailsByManager(UUID eventId);

    EventDetailsResponseForSystemAdmin getEventDetailsBySystemAdmin(UUID eventId);

    Page<EventSimpleResponseForHost> getEventsByHost(int pageNumber, int pageSize, String eventName, String status);
}
