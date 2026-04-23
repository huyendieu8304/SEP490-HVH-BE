package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.auth.request.ChangePasswordRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ChangePhoneRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ForgotPasswordRequest;
import com.sep490.g28.hvh.be.entity.User;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.AuthService;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {
    OtpService otpService;
    EmailService emailService;
    UserRepository userRepository;
    AuthClient authClient;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        //verify otp
        otpService.verifyVerifyForgotPasswordOtp(request.getEmail(), request.getOtp());

        //check whether the email is used for a account?
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new AppException(AppCommonErrorCode.EMAIL_NOT_USED)
        );
        //check active account
        if (!authClient.isAccountActive(user.getId())){
                throw new AppException(AppCommonErrorCode.ACCOUNT_INACTIVE);
        }
        //change password
        String newPassword = RandomStringUtil.random8AlphaNumeric();
        authClient.changePassword(user.getId(), newPassword);

        //send email to the user
        emailService.sendNewPasswordEmail(request.getEmail(), newPassword);
        log.info("Reset password for account successful, id={}", user.getId());
    }

    @Override
    public boolean checkAccountActive(UUID userId) {
        return authClient.isAccountActive(userId);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {

        String email = currentUserProvider.getEmail();
        //confirm the current password
        if (!authClient.checkOldPassword(email, request.getOldPassword())) {
            throw new AppException(AppCommonErrorCode.OLD_PASSWORD_INCORRECT);
        }

        //change password
        authClient.changePassword(currentUserProvider.getId(), request.getNewPassword());
        log.info("Change password for account successful, id={}", currentUserProvider.getId());
    }
//
//    @Override
//    public void changePhoneNumber(ChangePhoneRequest request) {
//
//        //verify otp
//        otpService.verifyVerifyChangePhoneNumberOtp(currentUserProvider.getEmail(), request.getOtp());
//
//        //change phone number
//        UUID currentAccountId = currentUserProvider.getId();
//        authClient.changePhoneNumber(currentAccountId, request.getNewPhoneNumber());
//        log.info("Change phone for account successful, id={}", currentAccountId);
//    }
}
