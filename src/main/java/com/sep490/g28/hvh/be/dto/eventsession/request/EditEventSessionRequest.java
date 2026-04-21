package com.sep490.g28.hvh.be.dto.eventsession.request;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.validation.EventSessionTime;
import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@EventSessionTime
@Valid
public class EditEventSessionRequest {

    UUID eventSessionId; //nullable

    @RequiredField(fieldName = "updateAction")
    EUpdateAction updateAction;

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
