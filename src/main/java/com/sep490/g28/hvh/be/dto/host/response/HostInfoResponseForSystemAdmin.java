package com.sep490.g28.hvh.be.dto.host.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostInfoResponseForSystemAdmin {
    UUID id;
    String cid; //citizen id
    String email;
    String phone;
    String fullName;
    Boolean gender; //1: male, 0: female
    LocalDate dob;
    String avatarUrl;
    String address;
    String detailAddress;
    OffsetDateTime createdAt;

    UUID orgId;
    String orgName;
    String orgAvatarUrl;
    Short orgAvgRating;
    int orgHostedEventCount;
}
