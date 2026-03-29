package com.sep490.g28.hvh.be.dto.event.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckEventCheckInCodeRequest {

    @NotBlank(message = "INVALID_CHECK_IN_CODE")
    @Pattern(regexp = "^\\d{6}$", message = "INVALID_CHECK_IN_CODE")
    String checkInCode;
}
