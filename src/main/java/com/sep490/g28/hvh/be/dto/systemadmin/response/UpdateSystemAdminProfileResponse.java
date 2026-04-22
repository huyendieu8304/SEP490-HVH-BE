package com.sep490.g28.hvh.be.dto.systemadmin.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateSystemAdminProfileResponse {
    private String avatarUploadUrl;
}
