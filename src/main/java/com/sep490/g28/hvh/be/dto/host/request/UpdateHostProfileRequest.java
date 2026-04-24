package com.sep490.g28.hvh.be.dto.host.request;

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
public class UpdateHostProfileRequest {

    @Length(max = 100, message = "INVALID_FULL_NAME")
    String fullName;

    boolean gender;

    LocalDate dob;

    @ImageFileExtension(fieldName = "Ảnh đại diện tình nguyện viên")
    String avatarExtension;

    @RequiredField(fieldName = "Địa chỉ")
    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String address;

    @RequiredField(fieldName = "Địa chỉ chi tiết")
    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String detailAddress;
}
