package com.sep490.g28.hvh.be.integration.cache;

/**
 * OTP service contract.
 *
 * <p>Defines operations for generating and verifying
 * one-time passwords (OTP) for different authentication flows.</p>
 */
public interface OtpService {

    /**
     * Generates an OTP for email verification during volunteer account registration.
     *
     * @param email target email
     * @return generated OTP
     */
    String getVolAccountRegistrationOtp(String email);

    /**
     * Verifies OTP for email verification during volunteer account registration.
     *
     * @param email    target email
     * @param inputOtp user-provided OTP
     * @return {@code true} if OTP is valid
     */
    boolean verifyVolAccountRegistrationOtp(String email, String inputOtp);

    /**
     * Generates an OTP for email verification during organization registration.
     *
     * @param email target email
     * @return generated OTP
     */
    String getOrgRegistrationOtp(String email);

    /**
     * Verifies OTP for email verification during organization registration.
     *
     * @param email target email
     * @return generated OTP
     */
    boolean verifyOrgRegistrationOtp(String email, String inputOtp);

    /**
     * Generates an OTP for forgot-password flow.
     *
     * @param email target email
     * @return generated OTP
     */
    String getVerifyForgotPasswordOtp(String email);

    /**
     * Verifies OTP for forgot-password flow.
     *
     * @param email    target email
     * @param inputOtp user-provided OTP
     * @return {@code true} if OTP is valid
     */
    boolean verifyVerifyForgotPasswordOtp(String email, String inputOtp);

    /**
     * Generates an OTP for change phone flow.
     *
     * @param email target email
     * @return generated OTP
     */
    String getVerifyChangePhoneNumberOtp(String email);

    /**
     * Verifies OTP for change phone number flow.
     *
     * @param email    target email
     * @param inputOtp user-provided OTP
     * @return {@code true} if OTP is valid
     */
    boolean verifyVerifyChangePhoneNumberOtp(String email, String inputOtp);
}
