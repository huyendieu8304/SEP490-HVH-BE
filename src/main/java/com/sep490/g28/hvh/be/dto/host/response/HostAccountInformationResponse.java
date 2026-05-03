package com.sep490.g28.hvh.be.dto.host.response;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class HostAccountInformationResponse {
    private UUID id;
    private String cid;
    private String email;
    private String phone;
    private String fullName;
    private Boolean gender;
    private LocalDate dob;
    private String avatarUrl;
    private String address;
    private String detailAddress;
}
