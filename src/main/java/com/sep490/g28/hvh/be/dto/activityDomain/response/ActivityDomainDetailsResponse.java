package com.sep490.g28.hvh.be.dto.activityDomain.response;

import com.sep490.g28.hvh.be.entity.ActivityDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
@Builder
public class ActivityDomainDetailsResponse {
    private Short id;
    private String name;
    Short specialSessionMaxTime;
    Boolean active;
    List<ActivitySubDomainDetailsResponse> activitySubDomainList;

    public static ActivityDomainDetailsResponse from(ActivityDomain ad) {
        return new ActivityDomainDetailsResponse(
                ad.getId(),
                ad.getName(),
                ad.getSpecialSessionMaxTime(),
                ad.getActive(),
                Optional.ofNullable(ad.getActivitySubDomains())
                        .map(list -> list.stream()
                                .map(ActivitySubDomainDetailsResponse::from)
                                .toList())
                        .orElse(Collections.emptyList())
        );
    }
}
