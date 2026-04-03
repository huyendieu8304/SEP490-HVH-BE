package com.sep490.g28.hvh.be.dto.eventrating.request;

import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RateEventRequest {
    @RequiredField(fieldName = "Id của đơn đăng kí")
    UUID eventApplicationId;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short organizationQualityRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short professionalismRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short workEnvironmentRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short valueImpactRating;

    @NotNull(message = "INVALID_RATING_VALUE")
    @Min(value = 1, message = "INVALID_RATING_VALUE")
    @Max(value = 5, message = "INVALID_RATING_VALUE")
    Short supportConnectionRating;
}
