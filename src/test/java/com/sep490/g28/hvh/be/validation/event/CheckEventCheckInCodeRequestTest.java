package com.sep490.g28.hvh.be.validation.event;

import com.sep490.g28.hvh.be.dto.eventapplication.request.CheckEventCheckInCodeRequest;
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

public class CheckEventCheckInCodeRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CheckEventCheckInCodeRequest validCheckEventCheckInCodeRequest() {
        CheckEventCheckInCodeRequest req = new CheckEventCheckInCodeRequest();
        req.setCheckInCode("123456");
        return req;
    }

    @Test
    void validRequest_shouldHaveNoViolation() {
        CheckEventCheckInCodeRequest req = validCheckEventCheckInCodeRequest();
        Set<ConstraintViolation<CheckEventCheckInCodeRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void checkInCodeEmpty_shouldFail() {
        CheckEventCheckInCodeRequest req = validCheckEventCheckInCodeRequest();
        req.setCheckInCode("");
        Set<ConstraintViolation<CheckEventCheckInCodeRequest>> v = validator.validate(req);

        assertEquals(2, v.size());
        assertEquals(ValidationErrorCode.INVALID_CHECK_IN_CODE.name(), v.iterator().next().getMessage());
    }

    @Test
    void checkInCodeIncorrectFormat_shouldFail() {
        CheckEventCheckInCodeRequest req = validCheckEventCheckInCodeRequest();
        req.setCheckInCode("12341245412");
        Set<ConstraintViolation<CheckEventCheckInCodeRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_CHECK_IN_CODE.name(), v.iterator().next().getMessage());
    }
}
