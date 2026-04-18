package com.sep490.g28.hvh.be.dto.eventapplication.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class CheckEventCheckInCodeResponse {
    private UUID eventId;
    private UUID eventSessionId;
    private UUID applicationId;
}
