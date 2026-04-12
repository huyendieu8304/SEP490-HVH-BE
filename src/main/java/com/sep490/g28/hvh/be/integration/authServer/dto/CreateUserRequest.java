package com.sep490.g28.hvh.be.integration.authServer.dto;

import java.util.Map;

public record CreateUserRequest (
        String email,
        String phone,
        String password,
        Boolean email_confirm,
        Map<String, Object> app_metadata
) {}
