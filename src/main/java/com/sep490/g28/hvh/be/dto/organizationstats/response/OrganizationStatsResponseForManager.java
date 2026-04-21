package com.sep490.g28.hvh.be.dto.organizationstats.response;

import com.sep490.g28.hvh.be.dto.host.payload.TopHostPayload;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationStatsResponseForManager {
    int year;
    int month;
    int completedEvents;
    int creditHours;
    int approvedApplications;
    int attendedApplications;
    List<TopHostPayload> topHostPayloads;
}
