package com.sep490.g28.hvh.be.dto.eventapplication.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class AccountCheckInStatusResponse {
    private UUID applicationId;
    private String eventName;
    private OffsetDateTime sessionEndDateTime;
    private UUID sessionId;
}
