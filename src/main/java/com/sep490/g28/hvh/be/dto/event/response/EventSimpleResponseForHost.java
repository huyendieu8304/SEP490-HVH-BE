package com.sep490.g28.hvh.be.dto.event.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSimpleResponseForHost {
    private UUID id;
    private String name;
    private String imageUrl;
    private String address;
    private LocalDate startDate;
    private LocalDate recruitmentEndDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    //todo numbers of registered/joined volunteers, numbers of shared moments, average rating
}
