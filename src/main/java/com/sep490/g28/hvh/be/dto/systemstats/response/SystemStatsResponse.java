package com.sep490.g28.hvh.be.dto.systemstats.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemStatsResponse {
    int year;
    int month;
    int verifiedVolunteers;
    int verifiedOrganizations;
    int completedEvents;
    int creditHours;
    int approvedApplications;
    int attendedApplications;
}
