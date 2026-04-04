package com.sep490.g28.hvh.be.dto.eventmoment.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ShareMomentResponse {
    private List<String> momentPicturesUrls;
}
