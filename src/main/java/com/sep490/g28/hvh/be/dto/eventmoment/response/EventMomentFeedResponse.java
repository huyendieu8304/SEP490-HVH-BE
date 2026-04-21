package com.sep490.g28.hvh.be.dto.eventmoment.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventMomentFeedResponse {
    private List<EventMomentFeedDetailsResponse> eventMoments;
    private String nextCursor;
    private boolean hasMore;
}
