package com.sep490.g28.hvh.be.validation.auth;

import com.sep490.g28.hvh.be.dto.auth.request.ChangePasswordRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class ChangePasswordRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void should_pass_when_valid() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("Abcdef1!");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations).isEmpty();
    }

    @Test
    void should_fail_when_oldPassword_null() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword(null);
        req.setNewPassword("Abcdef1!");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @Test
    void should_fail_when_oldPassword_blank() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("");
        req.setNewPassword("Abcdef1!");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @Test
    void should_fail_when_newPassword_null() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword(null);

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PASSWORD.name());
    }

    @Test
    void should_fail_when_newPassword_blank() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PASSWORD.name());
    }

    @Test
    void should_fail_when_newPassword_tooShort() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("Ab1!");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PASSWORD.name());
    }

    @Test
    void should_fail_when_newPassword_noDigit() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("Password!");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PASSWORD.name());
    }

    @Test
    void should_fail_when_newPassword_noSpecialChar() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldPass1!");
        req.setNewPassword("Password1");

        Set<ConstraintViolation<ChangePasswordRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PASSWORD.name());
    }
}
