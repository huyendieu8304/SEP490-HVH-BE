package com.sep490.g28.hvh.be.dto.organization.response;

import com.sep490.g28.hvh.be.constant.EOrgRegistrationStatus;
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
public class OrganizationRegistrationDetailsResponse {
    private UUID id;
    private String name;
    private Boolean dhaRegistered;
    private EOrgType orgType;
    private String orgIntroduction;
    private String managerFullName;
    private String managerCid;
    private String managerPhone;
    private String managerEmail;
    private String managerCidFrontUrl;
    private String managerCidBackUrl;
    private String managerCidHoldingUrl;
    private List<String> legalDocumentsUrls;
    private List<String> otherEvidencesUrls;
    private String applicationReason;
    private EOrgRegistrationStatus status;
    private String rejectionReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewedAt;
    private UUID adminId;
    private UUID organizationId;
    private UUID orgManagerId;
    private String note;

}
