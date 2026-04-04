package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckOutEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.request.RejectApplicationRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CheckEventCheckInCodeResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsStatusResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.ActualParticipantResponse;
import org.springframework.data.domain.Page;
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

    Page<EventApplicationsStatusResponse> getEventApplicationsStatus(int pageNumber, int pageSize, String inputStatus);

    CheckEventCheckInCodeResponse checkEventCheckInCode(CheckEventCheckInCodeRequest request);

    void quickCheckInEvent(QuickCheckInEventRequest request);

    void checkOutEvent(CheckOutEventRequest request);

    Page<ActualParticipantResponse> getActualParticipants(UUID sessionId, int pageNumber, int pageSize);

}
