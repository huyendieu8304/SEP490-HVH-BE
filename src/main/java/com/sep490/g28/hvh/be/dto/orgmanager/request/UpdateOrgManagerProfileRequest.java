package com.sep490.g28.hvh.be.dto.orgmanager.request;

import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.RequiredField;
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
public class UpdateOrgManagerProfileRequest {

    @Length(max = 100, message = "INVALID_FULL_NAME")
    String fullName;

    boolean gender;

    LocalDate dob;

    String avatarExtension;

    @Length(max = 50, message = "INVALID_STRING_LENGTH")
    String address;

    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String detailAddress;
}
