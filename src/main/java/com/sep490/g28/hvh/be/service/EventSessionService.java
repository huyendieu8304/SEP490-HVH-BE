package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventSessionPayload;
import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;

import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventSession;

import java.util.List;
import java.util.UUID;

public interface EventSessionService {

    void addEventSessionsForCreateEvent(
            Event event,
            List<EditEventSessionRequest> sessionRequests
    );

    void updateEventSessions(
            Event event,
            List<EditEventSessionRequest> sessionRequests
    );

    List<EventSession> findConflictSessionDateOfHost(UUID hostId, UUID checkedEventId, List<EventSession> checkedSessions);

    boolean checkAndResolveUpdateEventDateTime(
            Event event,
            UpdateEventRequest updateEventRequest,
            UpdateEventPayload updateEventPayload
    );

    List<EventSession> resolveUpdateEventSessions(
            Event event,
            List<EventSession> oldSessions,
            List<UpdateEventSessionPayload> newSessions
    );
}
