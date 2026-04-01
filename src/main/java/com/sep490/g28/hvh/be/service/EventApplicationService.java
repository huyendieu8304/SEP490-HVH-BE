package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventapplication.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventApplication;

import java.util.List;
import java.util.UUID;

public interface EventApplicationService {
    void applyEventSession(UUID sessionId);

    void approveApplication(UUID applicationId);

    void rejectApplication(UUID applicationId, RejectApplicationRequest request);

    void cancelApplication(UUID applicationId);

    EventApplicationsResponse getRegisteredParticipants(int pageNumber, int pageSize, UUID sessionId);

    List<EventApplication> cancelAllApplicationsOfEvent(Event event);
}
