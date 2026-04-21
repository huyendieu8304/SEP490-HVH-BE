package com.sep490.g28.hvh.be.dto.host.payload;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopHostPayload {
    UUID hostId;
    String fullName;
    String email;
    int totalEvent;
    int totalCreditHour;
}
