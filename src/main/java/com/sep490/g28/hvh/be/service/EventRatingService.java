package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;

import java.util.UUID;

public interface EventRatingService {
    void rateEvent(RateEventRequest request);

}
