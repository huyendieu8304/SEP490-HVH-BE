package com.sep490.g28.hvh.be.dto.eventapplication.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RejectApplicationRequest {

    @NotBlank(message = "INVALID_REJECTION_REASON")
    @NotNull(message = "INVALID_REJECTION_REASON")
    String rejectionReason;
}
