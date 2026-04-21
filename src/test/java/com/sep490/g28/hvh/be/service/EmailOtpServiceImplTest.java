package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.entity.User;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.impl.EmailOtpServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailOtpServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    AuthClient authClient;
    @Mock
    OtpService otpService;
    @Mock
    EmailService emailService;

    @InjectMocks
    EmailOtpServiceImpl emailOtpService;


    // ===== sendVerifyVolAccountRegistrationOtp =====
    @Test
    void sendVerifyVolAccountRegistrationOtp_success() {
        String email = "vol@mail.com";
        when(otpService.getVolAccountRegistrationOtp(email))
                .thenReturn("123456");

        emailOtpService.sendVerifyVolAccountRegistrationOtp(email);

        verify(otpService).getVolAccountRegistrationOtp(email);
        verify(emailService)
                .sendVolAccountRegistrationOtp(email, "123456");
    }

    @Test
    void sendVerifyVolAccountRegistrationOtp_emailUsed_shouldThrow() {
        String email = "exist@mail.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> emailOtpService.sendVerifyVolAccountRegistrationOtp(email)
        );

        assertEquals(AppCommonErrorCode.EMAIL_USED.getCode(), ex.getCode());

        verify(emailService, never()).sendVerifyForgotPasswordOtp(any(), any());
    }

    // ===== sendVerifyOrganizationRegistrationOtp =====
    @Test
    void sendVerifyOrganizationRegistrationOtp_success() {
        String email = "org@mail.com";
        when(otpService.getOrgRegistrationOtp(email))
                .thenReturn("654321");

        emailOtpService.sendVerifyOrganizationRegistrationOtp(email);

        verify(otpService).getOrgRegistrationOtp(email);
        verify(emailService)
                .sendOrgRegistrationOtp(email, "654321");
    }

    @Test
    void sendVerifyOrganizationRegistrationOtp_emailUsed_shouldThrow() {
        String email = "exist@mail.com";

        when(userRepository.existsByEmail(email))
                .thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> emailOtpService.sendVerifyOrganizationRegistrationOtp(email)
        );

        assertEquals(AppCommonErrorCode.EMAIL_USED.getCode(), ex.getCode());

        verify(emailService, never()).sendVerifyForgotPasswordOtp(any(), any());
    }

    // ===== sendVerifyForgotPasswordOtp =====
    // ===== tc01
    @Test
    void sendVerifyForgotPasswordOtp_success() {
        String email = "user@mail.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(authClient.isAccountActive(userId))
                .thenReturn(true);
        when(otpService.getVerifyForgotPasswordOtp(email))
                .thenReturn("999999");

        emailOtpService.sendVerifyForgotPasswordOtp(email);

        verify(otpService).getVerifyForgotPasswordOtp(email);
        verify(emailService)
                .sendVerifyForgotPasswordOtp(email, "999999");
    }

    // ===== tc2
    @Test
    void sendVerifyForgotPasswordOtp_emailNotUsed_shouldThrow() {
        String email = "notexist@mail.com";

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> emailOtpService.sendVerifyForgotPasswordOtp(email)
        );

        assertEquals(AppCommonErrorCode.EMAIL_NOT_USED.getCode(), ex.getCode());

        verify(authClient, never()).isAccountActive(any());
        verify(emailService, never()).sendVerifyForgotPasswordOtp(any(), any());
    }

    // ===== tc03
    @Test
    void sendVerifyForgotPasswordOtp_accountInactive_shouldThrow() {
        String email = "inactive@mail.com";
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail(email);

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));
        when(authClient.isAccountActive(userId))
                .thenReturn(false);

        AppException ex = assertThrows(
                AppException.class,
                () -> emailOtpService.sendVerifyForgotPasswordOtp(email)
        );

        assertEquals(AppCommonErrorCode.ACCOUNT_INACTIVE.getCode(), ex.getCode());

        verify(otpService, never()).getVerifyForgotPasswordOtp(any());
        verify(emailService, never()).sendVerifyForgotPasswordOtp(any(), any());
    }
}

