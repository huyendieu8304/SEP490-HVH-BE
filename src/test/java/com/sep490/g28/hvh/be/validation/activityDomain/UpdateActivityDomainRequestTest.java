package com.sep490.g28.hvh.be.validation.activityDomain;

import com.sep490.g28.hvh.be.dto.activityDomain.request.UpdateActivityDomainRequest;
import com.sep490.g28.hvh.be.dto.activityDomain.request.UpdateActivitySubDomainRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateActivityDomainRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private UpdateActivityDomainRequest validRequest() {
        UpdateActivityDomainRequest req = new UpdateActivityDomainRequest();
        req.setName("Domain A");
        req.setSpecialSessionMaxTime(Short.valueOf("8"));

        UpdateActivitySubDomainRequest subDomainUpdate_1 = new UpdateActivitySubDomainRequest();
        subDomainUpdate_1.setId(Short.valueOf("1"));
        subDomainUpdate_1.setName("Subdomain 1");
        subDomainUpdate_1.setAction("EDIT");

        UpdateActivitySubDomainRequest subDomainUpdate_2 = new UpdateActivitySubDomainRequest();
        subDomainUpdate_2.setId(Short.valueOf("2"));
        subDomainUpdate_2.setAction("DELETE");

        UpdateActivitySubDomainRequest subDomainUpdate_3 = new UpdateActivitySubDomainRequest();
        subDomainUpdate_3.setName("Subdomain 3");
        subDomainUpdate_3.setAction("ADD");

        req.setActivitySubDomainUpdateRequests(List.of(subDomainUpdate_1, subDomainUpdate_2, subDomainUpdate_3));

        return req;
    }

    @Test
    void validRequest_shouldHaveNoViolation() {
        Set<ConstraintViolation<UpdateActivityDomainRequest>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void subDomainUpdateNull_shouldPass() {
        UpdateActivityDomainRequest req = validRequest();
        req.setActivitySubDomainUpdateRequests(null);

        Set<ConstraintViolation<UpdateActivityDomainRequest>> violations =
                validator.validate(validRequest());

        assertTrue(violations.isEmpty());
    }

    @Test
    void should_fail_when_multiple_fields_invalid() {
        UpdateActivityDomainRequest req = validRequest();
        req.setName("");
        req.setSpecialSessionMaxTime(null);

        Set<ConstraintViolation<UpdateActivityDomainRequest>> violations =
                validator.validate(req);

        assertThat(violations).hasSize(2);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        ValidationErrorCode.MISSING_REQUIRED_FIELD.name(),
                        ValidationErrorCode.MISSING_REQUIRED_FIELD.name()
                );
    }

}
