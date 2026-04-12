package com.sep490.g28.hvh.be.integration.faceServer.dto;

public record FaceAuthenticationResponse (
        boolean success,
        String message,
        String name,
        float confidence,
        boolean liveness_passed,
        String reason
)
{}
