package com.sep490.g28.hvh.be.dto.event.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveEventRequest {

    @NotBlank(message = "INVALID_UUID")
    @UUID(message = "INVALID_UUID")
    String eventId;
}
