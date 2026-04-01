package com.sep490.g28.hvh.be.dto.event.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateEventResponse {
    private List<String> uploadUrls;
}