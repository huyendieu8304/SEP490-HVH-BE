package com.sep490.g28.hvh.be.dto.volunteer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateVolunteerAccountByAdminRequest {

    @NotBlank(message = "INVALID_EMAIL")
    @Email(message = "INVALID_EMAIL")
    String email;

    @NotBlank(message = "INVALID_PHONE")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$", message = "INVALID_PHONE")
    String phone;

    @NotBlank (message = "INVALID_CID")
    @Pattern(regexp = "^\\d{12}$", message = "INVALID_CID")
    String cid;

    /*
    - start with uppercase in each word, flowing by lowercase
    - between 2 words are a space
    - not include digit, special char, space in head and tail
     */
    @Pattern(regexp = "^[A-ZÀ-Ỹ][a-zà-ỹ]*(?:\\s[A-ZÀ-Ỹ][a-zà-ỹ]*)*$", message = "INVALID_FULL_NAME")
    @Length(max = 100, message = "INVALID_FULL_NAME")
    @NotNull(message = "INVALID_FULL_NAME")
    String fullName;

}
