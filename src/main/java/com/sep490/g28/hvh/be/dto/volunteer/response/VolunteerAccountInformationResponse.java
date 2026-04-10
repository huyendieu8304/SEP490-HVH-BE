package com.sep490.g28.hvh.be.dto.volunteer.response;

import com.sep490.g28.hvh.be.constant.EEducationLevel;
import com.sep490.g28.hvh.be.constant.EEmployStatus;
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
public class VolunteerAccountInformationResponse {
    private UUID id;
    private UUID vid;
    private String cid;
    private String email;
    private String phone;
    private boolean phoneVerified;
    private String nickname;
    private String fullName;
    private String bio;
    private boolean gender;
    private LocalDate dob;
    private Short level;
    private String avatarUrl;
    private String address;
    private String detailAddress;
    private EEmployStatus employStatus;
    private String workAddress;
    private EEducationLevel educationLevel;
    private String sid;
    private int creditScore;
    private int honorScore;
    private Short avgRating;
    private int activityCount;
}
