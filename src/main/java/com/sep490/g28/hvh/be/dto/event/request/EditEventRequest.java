package com.sep490.g28.hvh.be.dto.event.request;

import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.validation.RequiredField;
import com.sep490.g28.hvh.be.validation.ValidLatitude;
import com.sep490.g28.hvh.be.validation.ValidLongitude;
import com.sep490.g28.hvh.be.validation.ValidWard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EditEventRequest {

    //allowed null
    UUID eventId;

    @RequiredField(fieldName = "Tên sự kiện")
    String name;

    @Valid
    List<EditEventImageRequest> updateImages;

    @RequiredField(fieldName = "Mô tả sự kiện")
    String description;

    @NotBlank (message = "INVALID_ADDRESS")
    @ValidWard
    String address;

    @NotNull(message = "INVALID_EVENT_AUTO_APPROVE")
    Boolean autoApprove;

    @NotNull(message = "INVALID_EVENT_SUBDOMAIN_ID")
    @Positive(message = "INVALID_EVENT_SUBDOMAIN_ID")
    Short activitySubDomainId;

    @RequiredField(fieldName = "Đối tượng phục vụ")
    EServedTarget servedTarget;

    @RequiredField(fieldName = "Địa điểm phục vụ")
    EServingPlaceType servingPlaceType;

    //--------------------------------------------------------
    @RequiredField(fieldName = "Ngày kết thúc tuyển người")
    @Future(message = "INVALID_EVENT_RECRUITMENT_END_DATE")
    LocalDate recruitmentEndDate;

    @Valid
    List<EditEventSessionRequest> eventSessions;

    //--------------------------------------------------------
    @NotNull(message = "INVALID_LATITUDE")
    @ValidLatitude
    Double checkInPlaceLat;

    @NotNull(message = "INVALID_LONGITUDE")
    @ValidLongitude
    Double checkInPlaceLng;

    @Min(value = 300, message = "INVALID_EVENT_CHECKIN_ACCURACY_RANGE")
    @Max(value = 3000, message = "INVALID_EVENT_CHECKIN_ACCURACY_RANGE")
    int checkInPlaceAccuracyMeters;
}
