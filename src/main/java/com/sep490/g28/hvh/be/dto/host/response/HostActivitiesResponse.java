package com.sep490.g28.hvh.be.dto.host.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostActivitiesResponse {
    UUID eventId;
    String eventName;
    String eventAddress;
    String eventDetailAddress;

    UUID sessionId;
    OffsetDateTime sessionStartTime;
    OffsetDateTime sessionEndTime;

    public HostActivitiesResponse(
            UUID eventId,
            String eventName,
            String eventAddress,
            String eventDetailAddress,
            UUID sessionId,
            OffsetDateTime sessionStartTime,
            OffsetDateTime sessionEndTime) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventAddress = eventAddress;
        this.eventDetailAddress = eventDetailAddress;
        this.sessionId = sessionId;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
    }
}
