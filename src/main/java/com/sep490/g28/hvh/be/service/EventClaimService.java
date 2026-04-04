package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
import com.sep490.g28.hvh.be.dto.eventclaim.response.ClaimEventHourResponse;

public interface EventClaimService {
    ClaimEventHourResponse claimEventHour(ClaimEventHourRequest request);
}
