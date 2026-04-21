package com.sep490.g28.hvh.be.dto.event.response;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor

public class EventSimpleResponseForAdmin {
    private UUID id;
    private String name;
    private UUID organizationId;
    private String organizationName;
    private String address;
    private LocalDate startDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private EEventStatus status;
}
