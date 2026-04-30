package com.sep490.g28.hvh.be.dto.organization.response;

import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.constant.EOrganizationStatus;
import lombok.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrganizationSimpleResponseForSystemAdmin {
    private UUID id;
    private String name;
    private EOrgType orgType;
    private String avatarUrl;
    private int hostedEventCount;
    private int creditHour;
    private Short avgRating;
    private EOrganizationStatus status;
    private Set<String> activitySubDomains;
}
