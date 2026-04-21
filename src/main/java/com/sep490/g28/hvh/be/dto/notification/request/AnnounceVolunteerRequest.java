package com.sep490.g28.hvh.be.dto.notification.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AnnounceVolunteerRequest {
    @NotBlank(message = "INVALID_NOTIFICATION_TITLE")
    String title;

    @NotBlank(message = "INVALID_NOTIFICATION_BODY")
    String body;
}
