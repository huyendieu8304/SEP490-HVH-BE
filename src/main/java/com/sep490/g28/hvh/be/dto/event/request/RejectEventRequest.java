package com.sep490.g28.hvh.be.dto.event.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RejectEventRequest {
    @NotBlank(message = "INVALID_EVENT_REJECT_REASON")
    @NotNull(message = "INVALID_EVENT_REJECT_REASON")
    String reason;
}
