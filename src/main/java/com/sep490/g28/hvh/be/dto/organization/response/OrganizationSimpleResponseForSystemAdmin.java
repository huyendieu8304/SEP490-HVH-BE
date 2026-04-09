package com.sep490.g28.hvh.be.dto.organization.response;

import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.constant.EOrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class OrganizationSimpleResponseForSystemAdmin {
    private UUID id;
    private String name;
    private EOrgType orgType;
    private int hostedEventCount;
    private int creditHour;
    private Short avgRating;
    private EOrganizationStatus status;
    private Set<String> activitySubDomains;
}
