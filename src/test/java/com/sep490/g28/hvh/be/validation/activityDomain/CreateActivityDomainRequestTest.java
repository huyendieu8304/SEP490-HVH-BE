package com.sep490.g28.hvh.be.validation.activityDomain;

import com.sep490.g28.hvh.be.dto.activityDomain.request.CreateActivityDomainRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CreateActivityDomainRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private CreateActivityDomainRequest validRequest() {
        CreateActivityDomainRequest req = new CreateActivityDomainRequest();
        req.setName("Domain A");
        req.setSpecialSessionMaxTime(Short.valueOf("8"));
        req.setActivitySubDomain(List.of("Subdomain 1", "Subdomain 2"));
        return req;
    }

    //01
    @Test
    void validRequest_shouldHaveNoViolation() {
        Set<ConstraintViolation<CreateActivityDomainRequest>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void nameNull_shouldFail() {
        CreateActivityDomainRequest r = validRequest();
        r.setName(null);

        Set<ConstraintViolation<CreateActivityDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(), v.iterator().next().getMessage());
    }

    @Test
    void nameLengthEqualsMax_shouldPass() {
        CreateActivityDomainRequest r = validRequest();
        r.setName("a".repeat(50));

        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void nameLengthGreaterThanMax_shouldFail() {
        CreateActivityDomainRequest r = validRequest();
        r.setName("a".repeat(51));

        Set<ConstraintViolation<CreateActivityDomainRequest>> v = validator.validate(r);

        assertFalse(v.isEmpty());
    }

    @Test
    void specialSessionMaxTimeNull_shouldFail() {
        CreateActivityDomainRequest r = validRequest();
        r.setSpecialSessionMaxTime(null);

        Set<ConstraintViolation<CreateActivityDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.MISSING_REQUIRED_FIELD.name(), v.iterator().next().getMessage());
    }

    @Test
    void specialSessionMaxTimeEqualsMin_shouldPass() {
        CreateActivityDomainRequest r = validRequest();
        r.setSpecialSessionMaxTime(Short.valueOf("4"));

        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void specialSessionMaxTimeLessThanMin_shouldFail() {
        CreateActivityDomainRequest r = validRequest();
        r.setSpecialSessionMaxTime(Short.valueOf("3"));

        Set<ConstraintViolation<CreateActivityDomainRequest>> v = validator.validate(r);

        assertFalse(v.isEmpty());
    }

    @Test
    void specialSessionMaxTimeEqualsMax_shouldPass() {
        CreateActivityDomainRequest r = validRequest();
        r.setSpecialSessionMaxTime(Short.valueOf("12"));

        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void specialSessionMaxTimeGreaterThanMax_shouldFail() {
        CreateActivityDomainRequest r = validRequest();
        r.setSpecialSessionMaxTime(Short.valueOf("13"));

        Set<ConstraintViolation<CreateActivityDomainRequest>> v = validator.validate(r);

        assertFalse(v.isEmpty());
    }

    @Test
    void activitySubDomainNull_shouldPass() {
        CreateActivityDomainRequest r = validRequest();
        r.setActivitySubDomain(null);

        assertTrue(validator.validate(r).isEmpty());
    }

}
