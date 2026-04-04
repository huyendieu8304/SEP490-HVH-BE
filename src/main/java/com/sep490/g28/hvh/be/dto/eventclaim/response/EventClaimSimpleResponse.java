package com.sep490.g28.hvh.be.dto.eventclaim.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class EventClaimSimpleResponse {
    private UUID id;
    private UUID volunteerId;
    private String nickName;
    private String name;
    private String avatarUrl;
    private int creditScore;
    private int honorScore;
    private Short honorHours;
    private String reason;
}
