package com.sep490.g28.hvh.be.dto.eventapplication.response;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
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
    private LocalDate startDate;
    private EEventApplicationStatus status;
}
