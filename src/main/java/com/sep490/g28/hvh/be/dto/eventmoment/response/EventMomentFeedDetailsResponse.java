package com.sep490.g28.hvh.be.dto.eventmoment.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@Setter
@Builder
public class EventMomentFeedDetailsResponse {
    private UUID volunteerId;
    private String volNickName;
    private String volName;
    private String avatarUrl;

    private UUID eventId;
    private String eventName;
    private String eventAddress;
    private String eventDetailAddress;

    private UUID eventMomentId;
    private String momentContent;
    private List<String> momentPicturesUrls;
    private OffsetDateTime createdAt;

}
