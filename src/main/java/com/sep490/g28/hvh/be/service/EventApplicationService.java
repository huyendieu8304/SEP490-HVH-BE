package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EventApplicationService {
    void applyEventSession(UUID sessionId);

    void approveApplication(UUID applicationId);

    void rejectApplication(UUID applicationId, RejectApplicationRequest request);

    void cancelApplication(UUID applicationId);

    EventApplicationsResponse getRegisteredParticipants(int pageNumber, int pageSize, UUID sessionId);

    Page<EventApplicationsStatusResponse> getEventApplicationsStatus(int pageNumber, int pageSize, String inputStatus);

    CheckEventCheckInCodeResponse checkEventCheckInCode(CheckEventCheckInCodeRequest request);

    void quickCheckInEvent(QuickCheckInEventRequest request);
}
