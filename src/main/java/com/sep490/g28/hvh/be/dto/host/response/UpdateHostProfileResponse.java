package com.sep490.g28.hvh.be.dto.host.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateHostProfileResponse {
    private String avatarUploadUrl;
}
