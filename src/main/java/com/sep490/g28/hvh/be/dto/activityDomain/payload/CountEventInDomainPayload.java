package com.sep490.g28.hvh.be.dto.activityDomain.payload;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CountEventInDomainPayload {
    Short domainId;
    String domainName;
    int countEvent;
}
