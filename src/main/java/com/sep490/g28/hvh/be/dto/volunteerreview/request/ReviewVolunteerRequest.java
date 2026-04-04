package com.sep490.g28.hvh.be.dto.volunteerreview.request;

import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewVolunteerRequest {
    @RequiredField(fieldName = "Id của đơn đăng kí")
    UUID eventApplicationId;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short professionalAttitudeRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short responsibilityPunctualityRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short workEffectivenessRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short teamworkCommunicationRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short adaptabilityProblemSolvingRating;

    @NotBlank(message = "INVALID_VOL_REVIEW_COMMENT")
    @Length(max = 250, message = "INVALID_VOL_REVIEW_COMMENT")
    String comment;
}
