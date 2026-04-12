package com.sep490.g28.hvh.be.integration.faceServer.dto;

public record FaceRegisterResponse (
        boolean success,
        String message,
        String user_name,
        int total_frames_extracted,
        int frames_used,
        float embedding_quality,
        boolean face_detected,
        float total_identities
) {}
