package com.sep490.g28.hvh.be.validation.eventclaim;

import com.sep490.g28.hvh.be.dto.eventclaim.request.ClaimEventHourRequest;
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

public class ClaimEventHourRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ClaimEventHourRequest validRequest() {
        ClaimEventHourRequest req = new ClaimEventHourRequest();
        req.setEventSessionId(UUID.randomUUID().toString());
        req.setHonorHours((short) 5);
        req.setReason("Valid reason");
        req.setDetailReason("Valid detail reason");
        req.setEvidences(".jpg .png");
        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    // ================= eventSessionId =================

    @Test
    void should_fail_when_eventSessionId_blank() {
        ClaimEventHourRequest req = validRequest();
        req.setEventSessionId("");

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_UUID");
    }

    @Test
    void should_fail_when_eventSessionId_invalid_uuid() {
        ClaimEventHourRequest req = validRequest();
        req.setEventSessionId("invalid-uuid");

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_UUID");
    }

    // ================= honorHours =================

    @Test
    void should_fail_when_honorHours_null() {
        ClaimEventHourRequest req = validRequest();
        req.setHonorHours(null);

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // ================= reason =================

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   "
    })
    void should_fail_when_reason_blank(String reason) {
        ClaimEventHourRequest req = validRequest();
        req.setReason(reason);

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    @Test
    void should_fail_when_reason_too_long() {
        ClaimEventHourRequest req = validRequest();
        req.setReason("a".repeat(101));

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_STRING_LENGTH");
    }

    // ================= detailReason =================

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   "
    })
    void should_fail_when_detailReason_blank(String detail) {
        ClaimEventHourRequest req = validRequest();
        req.setDetailReason(detail);

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    @Test
    void should_fail_when_detailReason_too_long() {
        ClaimEventHourRequest req = validRequest();
        req.setDetailReason("a".repeat(301));

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_STRING_LENGTH");
    }

    // ================= evidences =================

    @Test
    void should_fail_when_evidences_blank() {
        ClaimEventHourRequest req = validRequest();
        req.setEvidences("");

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_FILE_TYPE");
    }

    @Test
    void should_fail_when_evidences_invalid_extension() {
        ClaimEventHourRequest req = validRequest();
        req.setEvidences("file.exe");

        Set<ConstraintViolation<ClaimEventHourRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_FILE_TYPE");
    }
}
