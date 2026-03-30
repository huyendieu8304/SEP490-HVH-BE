package com.sep490.g28.hvh.be.validation.event;

import com.sep490.g28.hvh.be.dto.event.request.SaveEventRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SaveEventRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private SaveEventRequest validSaveEventRequest() {
        SaveEventRequest req = new SaveEventRequest();
        req.setEventId(UUID.randomUUID().toString());
        return req;
    }

    @Test
    void validRequest_shouldHaveNoViolation() {
        Set<ConstraintViolation<SaveEventRequest>> violations =
                validator.validate(validSaveEventRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void eventIdNull_shouldFail() {
        SaveEventRequest r = validSaveEventRequest();
        r.setEventId(null);

        Set<ConstraintViolation<SaveEventRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_UUID.name(), v.iterator().next().getMessage());
    }

    @Test
    void eventIdNotUUIDFormat_shouldFail() {
        SaveEventRequest r = validSaveEventRequest();
        r.setEventId("anID");

        Set<ConstraintViolation<SaveEventRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_UUID.name(), v.iterator().next().getMessage());
    }
}
