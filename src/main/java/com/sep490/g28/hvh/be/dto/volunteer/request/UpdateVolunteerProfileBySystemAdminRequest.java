package com.sep490.g28.hvh.be.dto.volunteer.request;

import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateVolunteerProfileBySystemAdminRequest {

    @NotBlank(message = "INVALID_EMAIL")
    @Email(message = "INVALID_EMAIL")
    String email;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "INVALID_PHONE")
    String phone;

    @NotBlank (message = "INVALID_CID")
    @Pattern(regexp = "^\\d{12}$", message = "INVALID_CID")
    String cid;

    @RequiredField(fieldName = "Biệt danh")
    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String nickName;

    @Pattern(regexp = "^[A-ZÀ-Ỹ][a-zà-ỹ]*(?:\\s[A-ZÀ-Ỹ][a-zà-ỹ]*)*$", message = "INVALID_FULL_NAME")
    @Length(max = 100, message = "INVALID_FULL_NAME")
    String fullName;

    @RequiredField(fieldName = "Bio")
    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String bio;

    @RequiredField(fieldName = "Giới tính")
    boolean gender;

    @RequiredField(fieldName = "Ngày sinh")
    LocalDate dob;

    @ImageFileExtension(fieldName = "Ảnh đại diện tình nguyện viên")
    String avatarExtension;

    @RequiredField(fieldName = "Địa chỉ")
    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String address;

    @RequiredField(fieldName = "Địa chỉ chi tiết")
    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String detailAddress;

    @RequiredField(fieldName = "Tình trạng nghề nghiệp")
    @Length(max = 30, message = "INVALID_STRING_LENGTH")
    String employStatus;

    @RequiredField(fieldName = "Nơi làm việc/học tập")
    String workAddress;

    @RequiredField(fieldName = "Cấp độ giáo dục")
    @Length(max = 30, message = "INVALID_STRING_LENGTH")
    String educationLevel;

    @RequiredField(fieldName = "Mã học sinh/sinh viên")
    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String sid;

    @RequiredField(fieldName = "Mã thiết bị")
    String deviceId;
}
