package com.sep490.g28.hvh.be.dto.event.payload;

import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.entity.EventImage;
import com.sep490.g28.hvh.be.entity.EventSession;
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
    List<EventImage> eventImages;
    String description;
    Boolean autoApprove;
    EServingPlaceType servingPlaceType;

    // --------------------------------------------------------
    //CRITICAL FIELDS
    String address;
    String detailAddress;

    //event date time
    LocalDate recruitmentEndDate;
    List<EventSession> eventSessions;

    // checkin location
    Double checkInLocationLat;
    Double checkInLocationLng;
    int checkInLocationAccuracyMeters;

    LocalDate startDate;
    LocalDate endDate;
}
