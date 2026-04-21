package com.sep490.g28.hvh.be.validation.activityDomain;

import com.sep490.g28.hvh.be.dto.activityDomain.request.ChangeActivitySubDomainVisibilityRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChangeActivitySubDomainVisibilityRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private ChangeActivitySubDomainVisibilityRequest validRequest() {
        ChangeActivitySubDomainVisibilityRequest req = new ChangeActivitySubDomainVisibilityRequest();
        req.setIsVisible(true);
        return req;
    }

    @Test
    void validRequest_shouldHaveNoViolation() {
        Set<ConstraintViolation<ChangeActivitySubDomainVisibilityRequest>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void isVisibleNull_shouldFail() {
        ChangeActivitySubDomainVisibilityRequest r = validRequest();
        r.setIsVisible(null);

        Set<ConstraintViolation<ChangeActivitySubDomainVisibilityRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(), v.iterator().next().getMessage());
    }
}
