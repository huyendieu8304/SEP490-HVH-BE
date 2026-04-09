package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.entity.User;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.HostErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.EmailOtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailOtpServiceImpl implements EmailOtpService {
    UserRepository userRepository;
    AuthClient authClient;

    OtpService otpService;
    EmailService emailService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void sendVerifyVolAccountRegistrationOtp(String email){
        //check email used by any account
        if (userRepository.existsByEmail(email)) {
            throw new AppException(AppCommonErrorCode.EMAIL_USED);
        }
        String otp = otpService.getVolAccountRegistrationOtp(email);
        emailService.sendVolAccountRegistrationOtp(email, otp);
    }

    @Override
    public void sendVerifyOrganizationRegistrationOtp(String email){
        //check email used by any account
        if (userRepository.existsByEmail(email)) {
            throw new AppException(AppCommonErrorCode.EMAIL_USED);
        }
        String otp = otpService.getOrgRegistrationOtp(email);
        emailService.sendOrgRegistrationOtp(email, otp);
    }


    @Override
    public void sendVerifyForgotPasswordOtp(String email) {

        //check whether the email is used for a account?
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new AppException(AppCommonErrorCode.EMAIL_NOT_USED)
        );
        //check active account
        if (!authClient.isAccountActive(user.getId())){
            throw new AppException(AppCommonErrorCode.ACCOUNT_INACTIVE);
        }

        String otp = otpService.getVerifyForgotPasswordOtp(email);
        emailService.sendVerifyForgotPasswordOtp(email, otp);
    }

    @Override
    public void sendVerifyChangePhoneNumberOtp() {

        String userEmail = currentUserProvider.getEmail();

        String otp = otpService.getVerifyChangePhoneNumberOtp(userEmail);
        emailService.sendVerifyChangePhoneNumberOtp(userEmail, otp);
    }
}