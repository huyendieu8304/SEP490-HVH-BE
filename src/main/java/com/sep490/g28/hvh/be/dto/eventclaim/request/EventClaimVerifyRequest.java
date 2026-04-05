package com.sep490.g28.hvh.be.dto.eventclaim.request;

import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventClaimVerifyRequest {
    @RequiredField(fieldName = "Hành động chấp nhận khiếu nại")
    Boolean approve;
}
