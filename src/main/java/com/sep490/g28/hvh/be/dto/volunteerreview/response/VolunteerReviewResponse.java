package com.sep490.g28.hvh.be.dto.volunteerreview.response;


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
public class VolunteerReviewResponse {
    UUID id;
    String eventName;
    OffsetDateTime sessionStartDateTime;
    OffsetDateTime sessionEndDateTime;
    Short professionalAttitudeRating;
    Short responsibilityPunctualityRating;
    Short workEffectivenessRating;
    Short teamworkCommunicationRating;
    Short adaptabilityProblemSolvingRating;
    Short avgRating;
    String comment;

    public VolunteerReviewResponse(
            UUID id,
            String eventName,
            OffsetDateTime sessionStartDateTime,
            OffsetDateTime sessionEndDateTime,
            Short professionalAttitudeRating,
            Short responsibilityPunctualityRating,
            Short workEffectivenessRating,
            Short teamworkCommunicationRating,
            Short adaptabilityProblemSolvingRating,
            Short avgRating,
            String comment
    ) {
        this.id = id;
        this.eventName = eventName;
        this.sessionStartDateTime = sessionStartDateTime;
        this.sessionEndDateTime = sessionEndDateTime;
        this.professionalAttitudeRating = professionalAttitudeRating;
        this.responsibilityPunctualityRating = responsibilityPunctualityRating;
        this.workEffectivenessRating = workEffectivenessRating;
        this.teamworkCommunicationRating = teamworkCommunicationRating;
        this.adaptabilityProblemSolvingRating = adaptabilityProblemSolvingRating;
        this.avgRating = avgRating;
        this.comment = comment;
    }
}
