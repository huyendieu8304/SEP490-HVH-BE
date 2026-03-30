package com.sep490.g28.hvh.be.validation.event;

import com.sep490.g28.hvh.be.dto.event.request.CheckEventCheckInCodeRequest;
import com.sep490.g28.hvh.be.dto.event.request.QuickCheckInEventRequest;
import com.sep490.g28.hvh.be.dto.organization.request.OrganizationRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QuickCheckInEventRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private QuickCheckInEventRequest validQuickCheckInEventRequest() {
        QuickCheckInEventRequest req = new QuickCheckInEventRequest();
        req.setEventSessionId(UUID.randomUUID().toString());
        req.setDeviceId("1234567890");
        req.setApVersion("1.0.0");
        req.setOsVersion("1.0.0");
        req.setCurrentPlaceLat(1.0);
        req.setCurrentPlaceLng(2.0);
        return req;
    }

    @Test
    void validRequest_shouldHaveNoViolation() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        Set<ConstraintViolation<QuickCheckInEventRequest>> violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }

    @Test
    void eventSessionIdEmpty_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setEventSessionId("");
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(2, v.size());
        assertEquals(ValidationErrorCode.INVALID_UUID.name(), v.iterator().next().getMessage());
    }

    @Test
    void eventSessionIdNoUUIDFormat_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setEventSessionId("randomId");
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_UUID.name(), v.iterator().next().getMessage());
    }

    @Test
    void deviceIdNull_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setDeviceId(null);

        Set<ConstraintViolation<QuickCheckInEventRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @Test
    void apVersionNull_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setApVersion(null);

        Set<ConstraintViolation<QuickCheckInEventRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @Test
    void osVersionNull_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setOsVersion(null);

        Set<ConstraintViolation<QuickCheckInEventRequest>> violations =
                validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo(ValidationErrorCode.MISSING_REQUIRED_FIELD.name());
    }

    @Test
    void currentPlaceLatNull_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLat(null);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LATITUDE.name(), v.iterator().next().getMessage());
    }

    @Test
    void currentPlaceLatEqualMin_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLat(-90.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertTrue(v.isEmpty());
    }

    @Test
    void currentPlaceLatLessThanMin_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLat(-100.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LATITUDE.name(), v.iterator().next().getMessage());
    }

    @Test
    void currentPlaceLatEqualMax_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLat(90.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertTrue(v.isEmpty());
    }

    @Test
    void currentPlaceLatGreaterThanMax_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLat(100.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LATITUDE.name(), v.iterator().next().getMessage());
    }

    @Test
    void currentPlaceLngNull_shouldFail() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLng(null);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LONGITUDE.name(), v.iterator().next().getMessage());
    }

    @Test
    void currentPlaceLngEqualMin_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLng(-180.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertTrue(v.isEmpty());
    }

    @Test
    void currentPlaceLngLessThanMin_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLng(-190.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LONGITUDE.name(), v.iterator().next().getMessage());
    }

    @Test
    void currentPlaceLngEqualMax_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLng(180.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertTrue(v.isEmpty());
    }

    @Test
    void currentPlaceLngGreaterThanMax_shouldPass() {
        QuickCheckInEventRequest req = validQuickCheckInEventRequest();
        req.setCurrentPlaceLng(190.0);
        Set<ConstraintViolation<QuickCheckInEventRequest>> v = validator.validate(req);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_LONGITUDE.name(), v.iterator().next().getMessage());
    }
}
