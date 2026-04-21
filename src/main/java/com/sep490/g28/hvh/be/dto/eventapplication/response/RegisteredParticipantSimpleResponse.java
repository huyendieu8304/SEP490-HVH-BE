package com.sep490.g28.hvh.be.dto.eventapplication.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class  RegisteredParticipantSimpleResponse {
    private UUID applicationId;
    private UUID volunteerId;
    private String email;
    private String phone;
    private String nickName;
    private String name;
    private String avatarUrl;
    private String address;
    private int creditScore;
    private int honorScore;
    private OffsetDateTime createdAt;
}
