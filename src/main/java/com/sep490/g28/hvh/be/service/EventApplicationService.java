package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventapplication.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EventApplicationService {
    void applyEventSession(UUID sessionId);

    void approveApplication(UUID applicationId);

    void rejectApplication(UUID applicationId, RejectApplicationRequest request);

    void cancelApplication(UUID applicationId);

    EventApplicationsResponse getRegisteredParticipants(int pageNumber, int pageSize, UUID sessionId);

    Page<EventApplicationsStatusResponse> getEventApplicationsStatus(int pageNumber, int pageSize, String inputStatus);
}
