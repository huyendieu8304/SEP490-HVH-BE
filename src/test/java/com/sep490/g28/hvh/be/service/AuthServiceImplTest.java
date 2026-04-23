package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.auth.request.ChangePasswordRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ForgotPasswordRequest;
import com.sep490.g28.hvh.be.entity.User;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    OtpService otpService;
    @Mock
    UserRepository userRepository;
    @Mock
    AuthClient authClient;
    @Mock
    EmailService emailService;
    @Mock
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    AuthServiceImpl authService;

    private User user;

    @BeforeEach
    public void setUp() {

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@mail.com");
    }

    private ForgotPasswordRequest validForgotPasswordRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@mail.com");
        request.setOtp("123456");
        return request;
    }

    // ===== TC1
    @Test
    void forgotPassword_success() {
        ForgotPasswordRequest request = validForgotPasswordRequest();

        when(otpService.verifyVerifyForgotPasswordOtp(
                request.getEmail(), request.getOtp()))
                .thenReturn(true);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(authClient.isAccountActive(user.getId()))
                .thenReturn(true);

        authService.forgotPassword(request);

        verify(authClient).changePassword(eq(user.getId()), any());
        verify(emailService)
                .sendNewPasswordEmail(eq(request.getEmail()), any());
    }

    // ===== TC2
    @Test
    void forgotPassword_invalidOtp_shouldThrow() {
        ForgotPasswordRequest request = validForgotPasswordRequest();

        doThrow(new AppException(AppCommonErrorCode.OTP_INVALID))
                .when(otpService)
                .verifyVerifyForgotPasswordOtp(
                        request.getEmail(), request.getOtp());

        assertThrows(AppException.class,
                () -> authService.forgotPassword(request));

        verify(userRepository, never()).findByEmail(any());
        verify(authClient, never()).changePassword(any(), any());
        verify(emailService, never()).sendNewPasswordEmail(any(), any());
    }

    // ===== TC3
    @Test
    void forgotPassword_emailNotUsed_shouldThrow() {
        ForgotPasswordRequest request = validForgotPasswordRequest();

        when(otpService.verifyVerifyForgotPasswordOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> authService.forgotPassword(request));

        assertEquals(AppCommonErrorCode.EMAIL_NOT_USED.getCode(), ex.getCode());

        verify(authClient, never()).changePassword(any(), any());
        verify(emailService, never()).sendNewPasswordEmail(any(), any());
    }

    // ===== TC4
    @Test
    void forgotPassword_accountInactive_shouldThrow() {
        ForgotPasswordRequest request = validForgotPasswordRequest();

        when(otpService.verifyVerifyForgotPasswordOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);

        when(userRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(user));

        when(authClient.isAccountActive(user.getId()))
                .thenReturn(false);

        AppException ex = assertThrows(
                AppException.class,
                () -> authService.forgotPassword(request));

        assertEquals(AppCommonErrorCode.ACCOUNT_INACTIVE.getCode(), ex.getCode());

        verify(authClient, never()).changePassword(any(), any());
        verify(emailService, never()).sendNewPasswordEmail(any(), any());
    }

    //======= checkAccountActive ==========================================
    @Test
    void checkAccountActive_active_shouldReturnTrue() {
        UUID userId = UUID.randomUUID();

        when(authClient.isAccountActive(userId)).thenReturn(true);

        boolean result = authService.checkAccountActive(userId);

        assertTrue(result);
        verify(authClient).isAccountActive(userId);
    }

    @Test
    void checkAccountActive_inactive_shouldReturnFalse() {
        UUID userId = UUID.randomUUID();

        when(authClient.isAccountActive(userId)).thenReturn(false);

        boolean result = authService.checkAccountActive(userId);

        assertFalse(result);
        verify(authClient).isAccountActive(userId);
    }

    //======= changePassword ==========================================
    @Test
    void changePassword_valid_shouldChange() {
        String email = "test@gmail.com";
        UUID userId = UUID.randomUUID();

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("old");
        req.setNewPassword("new");

        when(currentUserProvider.getEmail()).thenReturn(email);
        when(currentUserProvider.getId()).thenReturn(userId);
        when(authClient.checkOldPassword(email, "old")).thenReturn(true);

        authService.changePassword(req);

        verify(authClient).changePassword(userId, "new");
    }

    @Test
    void changePassword_wrongOldPassword_shouldThrow() {
        String email = "test@gmail.com";

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("new");

        when(currentUserProvider.getEmail()).thenReturn(email);
        when(authClient.checkOldPassword(email, "wrong")).thenReturn(false);

        assertThrows(AppException.class, () -> authService.changePassword(req));

        verify(authClient, never()).changePassword(any(), any());
    }
}
