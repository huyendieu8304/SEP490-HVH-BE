package com.sep490.g28.hvh.be.validation.organization;
import com.sep490.g28.hvh.be.dto.organization.request.OrganizationRegistrationVerifyRequest;
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

public class OrganizationRegistrationVerifyRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private OrganizationRegistrationVerifyRequest validRequest() {
        OrganizationRegistrationVerifyRequest req =
                new OrganizationRegistrationVerifyRequest();
        req.setApprove(true);
        req.setRejectionReason(null);
        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        Set<ConstraintViolation<OrganizationRegistrationVerifyRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @Test
    void should_fail_when_approve_null() {
        OrganizationRegistrationVerifyRequest req = validRequest();
        req.setApprove(null);

        Set<ConstraintViolation<OrganizationRegistrationVerifyRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "   "
    })
    void should_fail_when_rejection_reason_blank(String reason) {
        OrganizationRegistrationVerifyRequest req = validRequest();
        req.setRejectionReason(reason);

        Set<ConstraintViolation<OrganizationRegistrationVerifyRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.INVALID_REJECTION_REASON.name());
    }


}
