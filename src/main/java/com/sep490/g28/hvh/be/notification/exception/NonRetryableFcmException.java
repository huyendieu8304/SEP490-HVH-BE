package com.sep490.g28.hvh.be.notification.exception;

/**
 * Custom exception for non retryable FirebaseCloudMessaging exception
 */
public class NonRetryableFcmException extends RuntimeException {
    public NonRetryableFcmException(String message) {
        super(message);
    }
}
