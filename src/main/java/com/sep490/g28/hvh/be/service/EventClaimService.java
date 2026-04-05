package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.request.EventClaimVerifyRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimDetailResponse;
import com.sep490.g28.hvh.be.dto.eventclaim.response.EventClaimSimpleResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface EventClaimService {
    ClaimEventHourResponse claimEventHour(ClaimEventHourRequest request);

    Page<EventClaimSimpleResponse> getEventClaims(int pageNumber, int pageSize, UUID eventId, UUID sessionId);

    EventClaimDetailResponse getEventClaimDetail(UUID claimId);

    void verifyEventClaim(UUID claimId, EventClaimVerifyRequest request);
}
