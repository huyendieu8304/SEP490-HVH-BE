package com.sep490.g28.hvh.be.dto.eventclaim.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ClaimEventHourResponse {
    private List<String> evidencesUrls;
}
