package com.sep490.g28.hvh.be.dto.event.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class EventSessionDetailsResponse {
    private UUID id;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private int expectedVolAmount;
    private int expectedSerAmount;
    private int approvedApplicationCount;
}
