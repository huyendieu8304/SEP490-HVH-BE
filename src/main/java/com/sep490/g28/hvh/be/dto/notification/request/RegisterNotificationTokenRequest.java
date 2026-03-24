package com.sep490.g28.hvh.be.dto.notification.request;

import com.sep490.g28.hvh.be.constant.EPlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * The request made by front end to register notification token
 */
@Getter
public class RegisterNotificationTokenRequest {

        @NotBlank(message = "INVALID_NOTIFICATION_TOKEN")
        @Size(min = 20, message = "INVALID_NOTIFICATION_TOKEN") //check garbage
        String token;

        EPlatform platform;

        @NotBlank(message = "INVALID_DEVICE_ID")
        @Size(min = 20, message = "INVALID_DEVICE_ID") //check garbage
        String deviceId;
}
