package com.sep490.g28.hvh.be.dto.organizationstats.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationCountHostsAndEventsResponse {
    int hostsCount;
    int recruitingEventsCount;
    int upcomingEventsCount;
    int ongoingEventsCount;

    public OrganizationCountHostsAndEventsResponse(
            Long recruitingEventsCount,
            Long upcomingEventsCount,
            Long ongoingEventsCount
    ) {
        this.recruitingEventsCount = recruitingEventsCount.intValue();
        this.upcomingEventsCount = upcomingEventsCount.intValue();
        this.ongoingEventsCount = ongoingEventsCount.intValue();
    }
}
