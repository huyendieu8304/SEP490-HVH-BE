package com.sep490.g28.hvh.be.validation.eventclaim;

import com.sep490.g28.hvh.be.dto.eventclaim.request.VerifyEventClaimRequest;
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

public class VerifyEventClaimRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private VerifyEventClaimRequest validRequest() {
        VerifyEventClaimRequest req = new VerifyEventClaimRequest();
        req.setApprove(true);
        return req;
    }

    @Test
    void should_pass_when_approve_valid() {
        Set<ConstraintViolation<VerifyEventClaimRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void should_fail_when_approve_null() {
        VerifyEventClaimRequest req = validRequest();
        req.setApprove(null);

        Set<ConstraintViolation<VerifyEventClaimRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }
}
