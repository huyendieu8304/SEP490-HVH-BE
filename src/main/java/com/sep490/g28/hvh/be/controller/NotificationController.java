package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.constant.ENotificationType;
import com.sep490.g28.hvh.be.dto.notification.request.RegisterNotificationTokenRequest;
import com.sep490.g28.hvh.be.dto.notification.response.NotificationResponse;
import com.sep490.g28.hvh.be.notification.entity.Notification;
import com.sep490.g28.hvh.be.notification.messageque.NotificationPublisher;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for notification
 */
@RestController
@RequestMapping("/api/v1")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class NotificationController {

    NotificationService notificationService;
    private final NotificationPublisher notificationPublisher;
    private final UserRepository userRepository;

    @PostMapping("/notifications/register-token")
    public ResponseEntity<Void> registerToken(@RequestBody RegisterNotificationTokenRequest request) {
        notificationService.registerNotificationToken(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/notifications/unregister-token")
    public ResponseEntity<Void> unregisterToken(@RequestParam String token) {
        notificationService.unregisterNotificationToken(token);
        return ResponseEntity.ok().build();
    }

//    @PostMapping("/send-to-topic")
//    public ResponseEntity<Void> sendNotiToTopic() {
//
//        Notification notification = new Notification();
//        notification.setId(UUID.randomUUID());
//        notification.setTopic("test-notification");
//        notification.setTitle("test-notification title");
//        notification.setBody("test-notification body muhaha");
//        notification.setData(Map.of("action", "OPEN"));
//        notification.setType(ENotificationType.EVENT_REJECTED_BY_MNG);
//        notificationPublisher.enqueueNotification(notification, null);
//        return ResponseEntity.ok().build();
//    }
//
    @PostMapping("/notifications/send-to-user")
    public ResponseEntity<Void> sendNotiToUser(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String body
    ) {
//        UUID userId = UUID.fromString("57fa7839-375d-4298-a319-cbea49d22ce7");
//        UUID userId = UUID.fromString("a67dabdc-ea8e-4c58-8312-c9aec971a38a");

        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(Map.of("action", "OPEN"));
        notification.setType(ENotificationType.VOL_APPLICATION_REJECTED);


        notificationPublisher.enqueueNotification(notification, userId);
        return ResponseEntity.ok().build();
    }

    //get the notification of a specific user
    @GetMapping("/notifications/user")
    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ORG_MANAGER','HOST','VOL')")
    public ResponseEntity<Page<NotificationResponse>> getLatestNotificationsOfUser(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 50, message = "INVALID_PAGE_SIZE")
            int pageSize
    ){
        return ResponseEntity.ok(notificationService.getLatestNotificationOfUser(pageSize, pageNumber));
    }


    @GetMapping("/notifications/user-topics")
    public ResponseEntity<Page<NotificationResponse>> getNotificationsOfUserTopic(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 50, message = "INVALID_PAGE_SIZE")
            int pageSize
    ) {
        return ResponseEntity.ok(notificationService.getLatestNotificationOfUserTopic(pageSize, pageNumber));

    }

}