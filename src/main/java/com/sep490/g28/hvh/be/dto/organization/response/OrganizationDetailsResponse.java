package com.sep490.g28.hvh.be.dto.organization.response;

import com.sep490.g28.hvh.be.constant.EOrgType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class OrganizationDetailsResponse {
    private UUID id;
    private String name;
    private Boolean dhaRegistered;
    private EOrgType orgType;
    private String orgIntroduction;
    private List<String> images;
    private OffsetDateTime createdAt;
    private UUID managerId;
    private String managerEmail;
    private String managerPhone;
    private Long totalHosts;
    private Long totalHonorHours;
    private String note;
}
