package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.service.EmailOtpService;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EmailOtpController {

    EmailOtpService emailOtpService;

    @PostMapping("/email-otp/verify-register-vol-acc")
    public ResponseEntity<String> sendVerifyRegisterVolAccountOtp(@RequestParam @Email(message = "INVALID_EMAIL") String email) {
        emailOtpService.sendVerifyVolAccountRegistrationOtp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-otp/verify-register-organization")
    public ResponseEntity<String> sendVerifyRegisterOrganizationOtp(@RequestParam @Email(message = "INVALID_EMAIL") String email) {
        emailOtpService.sendVerifyOrganizationRegistrationOtp(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-otp/verify-forgot-password")
    public ResponseEntity<String> sendVerifyForgotPasswordOtp(@RequestParam @Email String email) {
        emailOtpService.sendVerifyForgotPasswordOtp(email);
        return ResponseEntity.ok().build();
    }

//    @PreAuthorize("hasAnyRole('SYS_ADMIN', 'ORG_MANAGER','HOST','VOL')")
//    @PostMapping("/email-otp/verify-change-phone-number")
//    public ResponseEntity<String> sendVerifyChangePhoneNumberOtp() {
//        emailOtpService.sendVerifyChangePhoneNumberOtp();
//        return ResponseEntity.ok().build();
//    }

}