package com.sep490.g28.hvh.be.dto.event.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignHostToEventRequest {
    UUID hostId;
}
