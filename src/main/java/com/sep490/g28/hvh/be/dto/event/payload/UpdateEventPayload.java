package com.sep490.g28.hvh.be.dto.event.payload;

import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventPayload {
    //--------------------------------------------------------
    //NON-CRITICAL FIELDS
    List<UpdateEventImagePayload> eventImages;
    String description;
    Boolean autoApprove;
    EServingPlaceType servingPlaceType;

    // --------------------------------------------------------
    //CRITICAL FIELDS
    String address;
    String detailAddress;

    //event date time
    LocalDate recruitmentEndDate;
    List<UpdateEventSessionPayload> eventSessions;

    LocalDate startDate;
    LocalDate endDate;

    // checkin location
    Double checkInLocationLat;
    Double checkInLocationLng;
    Double checkInLocationAccuracyMeters;
}
