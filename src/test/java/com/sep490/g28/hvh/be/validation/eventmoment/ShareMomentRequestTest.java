package com.sep490.g28.hvh.be.validation.eventmoment;

import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ShareMomentRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ShareMomentRequest validRequest() {
        ShareMomentRequest req = new ShareMomentRequest();
        req.setEventSessionId(UUID.randomUUID().toString());
        req.setMomentContent("This is a valid content");
        req.setMomentPictures(".png .jpg");
        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_fail_when_eventSessionId_blank(String input) {
        ShareMomentRequest req = validRequest();
        req.setEventSessionId(input);

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_UUID");
    }

    @Test
    void should_fail_when_eventSessionId_invalid_uuid() {
        ShareMomentRequest req = validRequest();
        req.setEventSessionId("not-a-uuid");

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_UUID");
    }

    @Test
    void should_fail_when_momentContent_null() {
        ShareMomentRequest req = validRequest();
        req.setMomentContent(null);

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    @Test
    void should_fail_when_momentContent_exceed_length() {
        ShareMomentRequest req = validRequest();
        req.setMomentContent("a".repeat(501));

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_STRING_LENGTH");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_fail_when_momentPictures_blank(String input) {
        ShareMomentRequest req = validRequest();
        req.setMomentPictures(input);

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_FILE_TYPE");
    }

    @Test
    void should_fail_when_momentPictures_invalid_extension() {
        ShareMomentRequest req = validRequest();
        req.setMomentPictures("file.txt");

        Set<ConstraintViolation<ShareMomentRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_FILE_TYPE");
    }
}
