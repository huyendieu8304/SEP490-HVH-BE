package com.sep490.g28.hvh.be.dto.eventapplication.response;

import com.sep490.g28.hvh.be.entity.VolunteerReview;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CompletedApplicationResponse {
    UUID volunteerId;
    String fullName;
    String bio;
    String avatarUrl;
    String address;

    String nickname;
    String email;
    String phone;

    Integer creditScore;
    Integer honorScore;

    Short avgRating;
    UUID eventApplicationId;
    OffsetDateTime checkInTime;
    OffsetDateTime checkOutTime;

    boolean isReviewed = false;

    public CompletedApplicationResponse(
            UUID volunteerId,
            String fullName,
            String bio,
            String avatarUrl,
            String address,
            String nickname,
            String email,
            String phone,
            Integer creditScore,
            Integer honorScore,
            Short avgRating,
            UUID eventApplicationId,
            OffsetDateTime checkInTime,
            OffsetDateTime checkOutTime,
            VolunteerReview volunteerReview
    ) {
        this.volunteerId = volunteerId;
        this.fullName = fullName;
        this.bio = bio;
        this.avatarUrl = avatarUrl;
        this.address = address;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.creditScore = creditScore;
        this.honorScore = honorScore;
        this.avgRating = avgRating;
        this.eventApplicationId = eventApplicationId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;

        if (volunteerReview != null) {
            this.isReviewed = true;
        }
    }
}
