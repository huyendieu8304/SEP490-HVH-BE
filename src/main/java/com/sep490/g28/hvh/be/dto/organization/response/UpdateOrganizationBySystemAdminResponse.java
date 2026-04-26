package com.sep490.g28.hvh.be.dto.organization.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateOrganizationBySystemAdminResponse {
    private String avatarUploadUrl;

    private String coverUploadUrl;
}
