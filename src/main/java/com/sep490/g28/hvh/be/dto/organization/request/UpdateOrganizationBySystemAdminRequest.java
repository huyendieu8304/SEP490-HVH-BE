package com.sep490.g28.hvh.be.dto.organization.request;

import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.OrgType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateOrganizationBySystemAdminRequest {
    private String name;

    private Boolean dhaRegistered;

    @OrgType
    private String orgType;

    @Length(max = 500, message = "INVALID_STRING_LENGTH")
    private String orgIntroduction;

    @ImageFileExtension(fieldName = "Ảnh đại diện tổ chức")
    private String avatarImageExtension;

    @ImageFileExtension(fieldName = "Ảnh bìa tổ chức")
    private String coverImageExtension;
}
