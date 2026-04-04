package com.sep490.g28.hvh.be.dto.volunteer.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActualParticipantResponse {
    UUID volunteerId;
    String fullName;
    String bio;
    String avatarUrl;
    String address;

    Integer creditScore;
    Integer honorScore;

    Short avgRating;
    UUID eventApplicationId;
    OffsetDateTime checkInTime;
    OffsetDateTime checkOutTime;

    public ActualParticipantResponse(
            UUID volunteerId,
            String fullName,
            String bio,
            String avatarUrl,
            String address,
            Integer creditScore,
            Integer honorScore,
            Short avgRating,
            UUID eventApplicationId,
            OffsetDateTime checkInTime,
            OffsetDateTime checkOutTime
    ) {
        this.volunteerId = volunteerId;
        this.fullName = fullName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.address = address;
        this.creditScore = creditScore;
        this.honorScore = honorScore;
        this.avgRating = avgRating;
        this.eventApplicationId = eventApplicationId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }
}
