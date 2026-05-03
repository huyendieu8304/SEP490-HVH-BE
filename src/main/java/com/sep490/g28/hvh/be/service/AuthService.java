package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.auth.request.ChangePasswordRequest;
import com.sep490.g28.hvh.be.dto.auth.request.ForgotPasswordRequest;

import java.util.UUID;

public interface AuthService {
    void forgotPassword(ForgotPasswordRequest request);

    boolean checkAccountActive(UUID userId);

    void changePassword(ChangePasswordRequest request);

//    void changePhoneNumber(ChangePhoneRequest request);
}
