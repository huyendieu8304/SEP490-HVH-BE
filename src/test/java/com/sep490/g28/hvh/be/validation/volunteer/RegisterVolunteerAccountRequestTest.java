package com.sep490.g28.hvh.be.validation.volunteer;

import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class RegisterVolunteerAccountRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RegisterVolunteerAccountRequest validRequest() {
        RegisterVolunteerAccountRequest req = new RegisterVolunteerAccountRequest();
        req.setOtp("123456");
        req.setEmail("nguyenvanA@gmail.com");
        req.setPhone("0916234940");
        req.setFullName("Nguyễn Văn An");
        req.setCid("034309880903");
        req.setCidFrontFileExtension(".jpeg");
        req.setCidBackFileExtension(".png");
        req.setCidHoldingFileExtension(".jpg");
        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abcdef",
            "",
            "   ",
            "12345",
            "1234567",
    })
    void should_fail_when_otp_invalid() {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setOtp("123");

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

//        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_OTP.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "",
            "   ",
            "abc@",
            "@gmail.com"
    })
    void should_fail_when_email_invalid(String email) {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setEmail(email);

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

//        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_EMAIL.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0123",
            "0123456789",
            "   ",
            "",
            "09123456789",
            "09123456a89"
    })
    void should_fail_when_phone_invalid(String phone) {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setPhone(phone);

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

//        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_PHONE.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0123",
            "01234567891",
            "0123456789123",
            "0123456s789A123",
            "   ",
            "",
    })
    void should_fail_when_cid_invalid(String cid) {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setCid(cid);

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

//        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_CID.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ".pdf",
            ".html",
            "   ",
            "",
            ".gif",
            ".sdf",
    })
    void should_fail_when_image_mime_type_invalid(String mimeType) {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setCidFrontFileExtension(mimeType);

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

//        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_IMAGE_TYPE.name());
    }

    @Test
    void should_fail_when_multiple_fields_invalid() {
        RegisterVolunteerAccountRequest req = validRequest();
        req.setOtp("12345");
        req.setEmail("abc");
        req.setCidFrontFileExtension(".txt");

        Set<ConstraintViolation<RegisterVolunteerAccountRequest>> violations =
                validator.validate(req);

        assertThat(violations).hasSize(3);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        ValidationErrorCode.INVALID_OTP.name(),
                        ValidationErrorCode.INVALID_EMAIL.name(),
                        ValidationErrorCode.INVALID_IMAGE_TYPE.name()
                );
    }
}
