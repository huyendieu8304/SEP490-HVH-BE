package com.sep490.g28.hvh.be.dto.eventapplication.projection;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
public class ApplicationEventVolunteerProjection {
    UUID eventId;
    String eventName;

    UUID volunteerId;

    String checkInCode;
    UUID applicationId;

    public ApplicationEventVolunteerProjection(
            UUID eventId,
            String eventName,
            UUID volunteerId,
            String checkInCode,
            UUID applicationId
    ) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.volunteerId = volunteerId;
        this.checkInCode = checkInCode;
        this.applicationId = applicationId;
    }
}
