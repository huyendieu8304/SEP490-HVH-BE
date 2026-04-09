package com.sep490.g28.hvh.be.dto.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "INVALID_PASSWORD")
    String oldPassword;

    @NotBlank(message = "INVALID_PASSWORD")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*.,:;’])[A-Za-z\\d!@#$%^&*.,:;’]{8,}$",
            message = "INVALID_PASSWORD"
    )
    String newPassword;
}
