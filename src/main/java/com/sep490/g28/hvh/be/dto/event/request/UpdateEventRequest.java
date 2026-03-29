package com.sep490.g28.hvh.be.dto.event.request;

import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.validation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ValidLatLng
@AtLeastOneFieldNotNull
public class UpdateEventRequest {

    //--------------------------------------------------------
    //NON-CRITICAL FIELDS
    @Valid
    List<EditEventImageRequest> updateImages;

    @NotBlank(message = "INVALID_EVENT_DESCRIPTION")
    String description;

    Boolean autoApprove;

    EServingPlaceType servingPlaceType;

    // --------------------------------------------------------
    //CRITICAL FIELDS
    @NotBlank (message = "INVALID_ADDRESS")
    @ValidWard
    String address;

    @NotBlank(message = "INVALID_EVENT_DETAIL_ADDRESS")
    @Length(max = 200, message = "INVALID_EVENT_DETAIL_ADDRESS")
    String detailAddress;

    //event date time
    @Future(message = "INVALID_EVENT_RECRUITMENT_END_DATE")
    LocalDate recruitmentEndDate;

    @Valid
    List<EditEventSessionRequest> eventSessions;

    // checkin location
    @ValidLatitude
    Double checkInLocationLat;

    @ValidLongitude
    Double checkInLocationLng;

    @Min(value = 300, message = "INVALID_EVENT_CHECKIN_ACCURACY_RANGE")
    @Max(value = 3000, message = "INVALID_EVENT_CHECKIN_ACCURACY_RANGE")
    Integer checkInLocationAccuracyMeters;
}
