package com.sep490.g28.hvh.be.dto.host.request;

import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.ValidAge;
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

    @ValidAge
    LocalDate dob;

    @ImageFileExtension(fieldName = "Ảnh đại diện tình nguyện viên")
    String avatarExtension;

    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String address;

    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String detailAddress;
}
