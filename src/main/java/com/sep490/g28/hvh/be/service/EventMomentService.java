package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.ShareMomentResponse;

import java.util.UUID;

public interface EventMomentService {
    ShareMomentResponse shareMoment(ShareMomentRequest request);

    EventMomentFeedResponse getEventMomentsFeed(int pageNumber, int pageSize, String eventName);

    void deleteEventMoment(UUID eventMomentId);
}
