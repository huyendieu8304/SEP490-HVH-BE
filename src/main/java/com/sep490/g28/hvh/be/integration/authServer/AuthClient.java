package com.sep490.g28.hvh.be.integration.authServer;

import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.integration.authServer.dto.UserResponse;

import java.util.UUID;

/**
 * Authentication service contract.
 *
 * <p>Defines operations for communication with authentication server</p>
 */
public interface AuthClient {
    /**
     * Creates a new user account with the given role and credentials.
     *
     * @param role     role assigned to the user
     * @param email    unique email of the user
     * @param password raw password
     * @param phone    phone number stored as metadata
     * @return unique identifier of the created user
     */
    UUID createAccount(ERole role, String email, String password, String phone);


    void changePassword(UUID accountId, String newPassword);

    void changePhoneNumber(UUID accountId, String newPhone);

    UserResponse getAccountInfo(UUID accountId);

    boolean isAccountActive(UUID accountId);

    boolean checkOldPassword(String userEmail, String oldPassword);
}
