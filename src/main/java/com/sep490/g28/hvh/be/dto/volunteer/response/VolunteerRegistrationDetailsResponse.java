package com.sep490.g28.hvh.be.dto.volunteer.response;

import com.sep490.g28.hvh.be.constant.EVolunteerVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class VolunteerRegistrationDetailsResponse {
    private UUID id;
    private String cid;
    private String email;
    private String phone;
    private String fullName;
    private String cidFrontUrl;
    private String cidBackUrl;
    private String cidHoldingUrl;
    private EVolunteerVerificationStatus status;
    private String rejectionReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime reviewAt;
    private String adminId;
    private String adminEmail;
    private String volunteerId;
    private String volunteerEmail;
    private String note;

}
