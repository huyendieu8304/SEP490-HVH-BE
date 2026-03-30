package com.sep490.g28.hvh.be.dto.event.payload;

import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.entity.EventImage;
import com.sep490.g28.hvh.be.entity.EventSession;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.locationtech.jts.geom.Point;

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
    LocalDate startDate;
    LocalDate endDate;

    // checkin location
    Point checkInLocation;
    Double checkInLocationAccuracyMeters;

}
