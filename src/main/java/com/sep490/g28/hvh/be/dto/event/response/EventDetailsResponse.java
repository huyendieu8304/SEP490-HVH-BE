package com.sep490.g28.hvh.be.dto.event.response;

import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class EventDetailsResponse {
    private UUID id;
    private String name;
    private List<String> imageUrls;
    private String description;
    private String address;
    private String detailAddress;
    private String activitySubDomain;
    private EServedTarget servedTarget;
    private EServingPlaceType servingPlaceType;
    private LocalDate startDate;
    private LocalDate recruitmentEndDate;
    private Double latCheckInLocation;
    private Double lngCheckInLocation;
    private Double checkInAccuracyMeters;
    private String hostPhone;
    private String orgName;
    private List<EventSessionDetailsResponse> eventSessions;
}
