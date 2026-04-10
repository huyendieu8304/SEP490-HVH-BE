package com.sep490.g28.hvh.be.dto.volunteer.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class VolunteerPublicInformationResponse {
    private String fullName;
    private String nickname;
    private String bio;
    private LocalDate dob;
    private String avatarUrl;
    private int creditScore;
    private Short avgRating;
    private int activityCount;
    private List<String> certificatesUrls;
}
