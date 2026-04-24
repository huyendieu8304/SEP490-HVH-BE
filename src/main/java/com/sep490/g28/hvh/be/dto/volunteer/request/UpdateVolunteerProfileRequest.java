package com.sep490.g28.hvh.be.dto.volunteer.request;

import com.sep490.g28.hvh.be.constant.EEducationLevel;
import com.sep490.g28.hvh.be.constant.EEmployStatus;
import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.RequiredField;
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
public class UpdateVolunteerProfileRequest {

    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String nickName;

    @Length(max = 100, message = "INVALID_FULL_NAME")
    String fullName;

    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String bio;

    boolean gender;

    LocalDate dob;

    @ImageFileExtension(fieldName = "Ảnh đại diện tình nguyện viên")
    String avatarExtension;

    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String address;

    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String detailAddress;

    @Length(max = 30, message = "INVALID_STRING_LENGTH")
    String employStatus;

    String workAddress;

    @Length(max = 30, message = "INVALID_STRING_LENGTH")
    String educationLevel;

    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String sid;

    String deviceId;
}
