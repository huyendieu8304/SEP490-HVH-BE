package com.sep490.g28.hvh.be.dto.eventclaim.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class EventClaimDetailResponse {
    private UUID id;
    private UUID sessionId;
    private UUID volunteerId;
    private String email;
    private String phone;
    private String nickName;
    private String name;
    private String avatarUrl;
    private String address;
    private int creditScore;
    private int honorScore;
    private Short honorHours;
    private String reason;
    private String detailReason;
    private List<String> evidencesUrls;
}
