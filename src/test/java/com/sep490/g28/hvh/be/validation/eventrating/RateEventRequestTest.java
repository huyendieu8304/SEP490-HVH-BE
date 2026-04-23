package com.sep490.g28.hvh.be.validation.eventrating;

import com.sep490.g28.hvh.be.dto.eventrating.request.RateEventRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RateEventRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private RateEventRequest validRequest() {
        RateEventRequest req = new RateEventRequest();
        req.setEventApplicationId(UUID.randomUUID());

        req.setOrganizationQualityRating((short) 5);
        req.setProfessionalismRating((short) 5);
        req.setWorkEnvironmentRating((short) 5);
        req.setValueImpactRating((short) 5);
        req.setSupportConnectionRating((short) 5);

        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        var violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    // ================= eventApplicationId =================

    @Test
    void should_fail_when_eventApplicationId_null() {
        var req = validRequest();
        req.setEventApplicationId(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // ================= rating range (1-5) =================

    @ParameterizedTest
    @ValueSource(shorts = {0, 6, -1, 100})
    void should_fail_when_organizationQualityRating_out_of_range(short value) {
        var req = validRequest();
        req.setOrganizationQualityRating(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_RATING_VALUE");
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 6})
    void should_fail_when_professionalismRating_invalid(short value) {
        var req = validRequest();
        req.setProfessionalismRating(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_RATING_VALUE");
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 6})
    void should_fail_when_workEnvironmentRating_invalid(short value) {
        var req = validRequest();
        req.setWorkEnvironmentRating(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_RATING_VALUE");
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 6})
    void should_fail_when_valueImpactRating_invalid(short value) {
        var req = validRequest();
        req.setValueImpactRating(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_RATING_VALUE");
    }

    @ParameterizedTest
    @ValueSource(shorts = {0, 6})
    void should_fail_when_supportConnectionRating_invalid(short value) {
        var req = validRequest();
        req.setSupportConnectionRating(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_RATING_VALUE");
    }

    // ================= multiple fields =================

    @Test
    void should_fail_when_multiple_fields_invalid() {
        var req = validRequest();

        req.setOrganizationQualityRating((short) 0);
        req.setProfessionalismRating((short) 6);
        req.setValueImpactRating(null);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(3);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("INVALID_RATING_VALUE");
    }
}
