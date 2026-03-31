package com.sep490.g28.hvh.be.dto.event.payload;

import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventSessionPayload {

    UUID id; //nullable

    @RequiredField(fieldName = "Thời gian bắt đầu")
    @Future(message = "INVALID_EVENT_SESSION_DATE")
    OffsetDateTime startDateTime; // check-in time

    @RequiredField(fieldName = "Thời gian kết thúc")
    @Future(message = "INVALID_EVENT_SESSION_DATE")
    OffsetDateTime endDateTime;   // check-out time

    @NotNull(message = "INVALID_EVENT_EXPECTED_VOL_AMOUNT")
    @PositiveOrZero( message = "INVALID_EVENT_EXPECTED_VOL_AMOUNT")
    @Max(value = 10000000, message = "INVALID_EVENT_EXPECTED_VOL_AMOUNT")
    Integer  expectedVolAmount;

    @NotNull(message = "INVALID_EVENT_EXPECTED_SER_AMOUNT")
    @PositiveOrZero( message = "INVALID_EVENT_EXPECTED_SER_AMOUNT")
    @Max(value = 10000000, message = "INVALID_EVENT_EXPECTED_SER_AMOUNT")
    Integer  expectedSerAmount;
}
