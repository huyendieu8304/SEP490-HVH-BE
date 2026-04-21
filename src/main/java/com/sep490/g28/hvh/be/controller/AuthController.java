package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.auth.request.ChangePasswordRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ChangePhoneRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ForgotPasswordRequest;
import com.sep490.g28.hvh.be.service.AuthService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class AuthController {

    AuthService authService;

    @PutMapping("/auth/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ORG_MANAGER','HOST','VOL')")
    @PutMapping("/auth/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request){
        authService.changePassword(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ORG_MANAGER','HOST','VOL')")
    @PutMapping("/auth/change-phone")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePhoneRequest request){
        authService.changePhoneNumber(request);
        return ResponseEntity.ok().build();
    }
}
