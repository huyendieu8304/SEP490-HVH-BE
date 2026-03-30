package com.sep490.g28.hvh.be.dto.event.request;

import com.sep490.g28.hvh.be.validation.RequiredField;
import com.sep490.g28.hvh.be.validation.ValidLatitude;
import com.sep490.g28.hvh.be.validation.ValidLongitude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuickCheckInEventRequest {

    @NotBlank(message = "INVALID_UUID")
    @UUID(message = "INVALID_UUID")
    String eventSessionId;

    @RequiredField(fieldName = "Device ID")
    String deviceId;

    @RequiredField(fieldName = "AP Version")
    String apVersion;

    @RequiredField(fieldName = "OS Version")
    String osVersion;

    //--------------------------------------------------------
    @NotNull(message = "INVALID_LATITUDE")
    @ValidLatitude
    Double currentPlaceLat;

    @NotNull(message = "INVALID_LONGITUDE")
    @ValidLongitude
    Double currentPlaceLng;

}
