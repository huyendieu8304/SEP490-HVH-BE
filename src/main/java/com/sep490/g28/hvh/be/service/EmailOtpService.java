package com.sep490.g28.hvh.be.service;

public interface EmailOtpService {
    void sendVerifyVolAccountRegistrationOtp(String email);
    void sendVerifyOrganizationRegistrationOtp(String email);
    void sendVerifyForgotPasswordOtp (String email);
    void sendVerifyChangePhoneNumberOtp();
}