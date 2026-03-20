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
public class OrganizationDetailsResponseForSystemAdmin {
    private UUID id;
    private String name;
    private Boolean dhaRegistered;
    private EOrgType orgType;
    private String orgIntroduction;
    private String avatarImageUrl;
    private String coverImageUrl;
    private List<String> legalDocumentUrls;
    private List<String> otherEvidencesUrls;
    private OffsetDateTime createdAt;
    private UUID managerId;
    private String managerName;
    private String managerEmail;
    private String managerPhone;
    private String managerCID;
    private Long totalHosts;
    private Long totalHonorHours;
    private String note;
}
