package com.sep490.g28.hvh.be.validation.activityDomain;

import com.sep490.g28.hvh.be.dto.activityDomain.request.UpdateActivitySubDomainRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class UpdateActivitySubDomainRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UpdateActivitySubDomainRequest validRequest1() {
        UpdateActivitySubDomainRequest req = new UpdateActivitySubDomainRequest();
        req.setId(Short.valueOf("1"));
        req.setName("Subdomain 1");
        req.setAction("EDIT");
        return req;
    }

    private UpdateActivitySubDomainRequest validRequest3() {
        UpdateActivitySubDomainRequest req = new UpdateActivitySubDomainRequest();
        req.setName("Subdomain 1");
        req.setAction("ADD");
        return req;
    }

    @Test
    void validRequest1_shouldHaveNoViolation() {
        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> violations =
                validator.validate(validRequest1());

        assertTrue(violations.isEmpty());
    }

    @Test
    void validRequest3_shouldHaveNoViolation() {
        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> violations =
                validator.validate(validRequest3());

        assertTrue(violations.isEmpty());
    }

    @Test
    void nameLengthEqualsMax_shouldPass() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setName("a".repeat(50));

        assertTrue(validator.validate(r).isEmpty());
    }

    @Test
    void nameLengthGreaterThanMax_shouldFail() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setName("a".repeat(51));

        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> v = validator.validate(r);

        assertFalse(v.isEmpty());
    }

    @Test
    void nameNullWhenActionIsEdit_shouldFail() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setName(null);
        r.setAction("EDIT");

        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_SUBDOMAIN_UPDATE.name(), v.iterator().next().getMessage());
    }

    @Test
    void idNullWhenActionIsEdit_shouldFail() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setId(null);
        r.setAction("EDIT");

        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_SUBDOMAIN_UPDATE.name(), v.iterator().next().getMessage());
    }

    @Test
    void idNullWhenActionIsDelete_shouldFail() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setId(null);
        r.setAction("DELETE");

        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_SUBDOMAIN_UPDATE.name(), v.iterator().next().getMessage());
    }

    @Test
    void nameNullWhenActionIsAdd_shouldFail() {
        UpdateActivitySubDomainRequest r = validRequest1();
        r.setName(null);
        r.setAction("ADD");

        Set<ConstraintViolation<UpdateActivitySubDomainRequest>> v = validator.validate(r);

        assertEquals(1, v.size());
        assertEquals(ValidationErrorCode.INVALID_SUBDOMAIN_UPDATE.name(), v.iterator().next().getMessage());
    }
}
