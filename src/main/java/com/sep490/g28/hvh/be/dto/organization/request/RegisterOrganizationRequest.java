package com.sep490.g28.hvh.be.dto.organization.request;

import com.sep490.g28.hvh.be.validation.AllowedFileExtension;
import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.OrgType;
import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterOrganizationRequest {

    @NotBlank(message = "INVALID_OTP")
    @Pattern(regexp = "^\\d{6}$", message = "INVALID_OTP")
    String otp;

    @RequiredField(fieldName = "Tên tổ chức")
    String name;

    @RequiredField(fieldName = "Trạng thái đăng ký")
    Boolean dhaRegistered;

    @NotBlank(message = "INVALID_ORG_TYPE")
    @OrgType
    String orgType;

    @RequiredField(fieldName = "Giới thiệu về tổ chức")
    @Length(max = 500, message = "INVALID_STRING_LENGTH")
    String orgIntroduction;

    @Pattern(regexp = "^[A-ZÀ-Ỹ][a-zà-ỹ]*(?:\\s[A-ZÀ-Ỹ][a-zà-ỹ]*)*$", message = "INVALID_FULL_NAME")
    @Length(max = 100, message = "INVALID_FULL_NAME")
    String managerFullName;

    @NotBlank(message = "INVALID_CID")
    @Pattern(regexp = "^\\d{12}$", message = "INVALID_CID")
    String managerCid;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "INVALID_PHONE")
    String managerPhone;

    @NotBlank(message = "INVALID_EMAIL")
    @Email(message = "INVALID_EMAIL")
    String managerEmail;

    @NotBlank(message = "INVALID_IMAGE_TYPE")
    @ImageFileExtension(fieldName = "Ảnh mặt trước căn cước công dân")
    String managerCidFrontExtension;

    @NotBlank(message = "INVALID_IMAGE_TYPE")
    @ImageFileExtension(fieldName = "Ảnh mặt sau căn cước công dân")
    String managerCidBackExtension;

    @NotBlank(message = "INVALID_IMAGE_TYPE")
    @ImageFileExtension(fieldName = "Ảnh cầm căn cước công dân")
    String managerCidHoldingExtension;

    @NotBlank(message = "INVALID_FILE_TYPE")
    @AllowedFileExtension(fieldName = "Tài liệu chứng minh tổ chức")
    String legalDocumentsExtensions;

    @AllowedFileExtension(fieldName = "Những tài liệu liên quan khác")
    String otherEvidencesExtensions;

    @RequiredField(fieldName = "Lý do đăng ký")
    String applicationReason;
}
