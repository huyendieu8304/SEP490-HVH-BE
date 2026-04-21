package com.sep490.g28.hvh.be.dto.eventapplication.response;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.event.response.EventSessionDetailsResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class EventApplicationsStatusResponse {
    private UUID id;
    private UUID eventId;
    private String name;
    private String imageUrl;
    private String address;
    private String detailAddress;
    private LocalDate startDate;
    private EEventApplicationStatus status;
    private EventSessionDetailsResponse session;
}
