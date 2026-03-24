package com.sep490.g28.hvh.be.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserToTopicSubscriptionMessage {
    private UUID userId;
    private String topic;
}
