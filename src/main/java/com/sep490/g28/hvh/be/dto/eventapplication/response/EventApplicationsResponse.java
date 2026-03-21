package com.sep490.g28.hvh.be.dto.eventapplication.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class EventApplicationsResponse {
    List<RegisteredParticipantSimpleResponse> registeredParticipants;
    private String nextCursor;
    private boolean hasMore;
}
