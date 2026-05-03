package com.sep490.g28.hvh.be.dto.volunteer.response;

import com.sep490.g28.hvh.be.constant.EAccountStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
public class VolunteerSimpleResponseForAdmin {
    UUID id;
    UUID vid; //volunteer id
    String avatarUrl;
    String fullName;
    String cid;
    String phone;
    String email;
    LocalDate dob;
    Integer activityCount;
    Short avgRating;
    Integer creditScore;
    EAccountStatus status;
    String address;
    String detailAddress;

    public VolunteerSimpleResponseForAdmin(
            UUID id,
            UUID vid,
            String avatarUrl,
            String fullName,
            String cid,
            String phone,
            String email,
            LocalDate dob,
            Integer activityCount,
            Short avgRating,
            Integer creditScore,
            EAccountStatus status,
            String address,
            String detailAddress
    ) {
        this.id = id;
        this.vid = vid;
        this.avatarUrl = avatarUrl;
        this.fullName = fullName;
        this.cid = cid;
        this.phone = phone;
        this.email = email;
        this.dob = dob;
        this.activityCount = activityCount;
        this.avgRating = avgRating;
        this.creditScore = creditScore;
        this.status = status;
        this.address = address;
        this.detailAddress = detailAddress;
    }
}
