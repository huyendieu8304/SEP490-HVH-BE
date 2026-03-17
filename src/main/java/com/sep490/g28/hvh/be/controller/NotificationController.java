package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.notification.dto.RegisterNotificationTokenRequest;
import com.sep490.g28.hvh.be.notification.messageque.NotificationPublisher;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.NotificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for notification
 */
@RestController
@RequestMapping("/api/v1/notification")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class NotificationController {

    NotificationService notificationService;
    private final NotificationPublisher notificationPublisher;
    private final UserRepository userRepository;

    @PostMapping("/register-token")
    public ResponseEntity<Void> registerToken(@RequestBody RegisterNotificationTokenRequest request) {
        notificationService.registerNotificationToken(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/unregister-token")
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
//    @PostMapping("/send-to-user")
//    public ResponseEntity<Void> sendNotiToUser(@RequestParam UUID userId) {
////        UUID userId = UUID.fromString("57fa7839-375d-4298-a319-cbea49d22ce7");
////        UUID userId = UUID.fromString("a67dabdc-ea8e-4c58-8312-c9aec971a38a");
//
//        Notification notification = new Notification();
//        notification.setId(UUID.randomUUID());
//        notification.setTitle("test-notification send to user");
//        notification.setBody("test-notification body muhaha");
//        notification.setData(Map.of("action", "OPEN"));
//        notification.setType(ENotificationType.EVENT_REJECTED_BY_MNG);
//
//
//        notificationPublisher.enqueueNotification(notification, userId);
//        return ResponseEntity.ok().build();
//    }

}