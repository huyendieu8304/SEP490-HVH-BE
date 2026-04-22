package com.sep490.g28.hvh.be.validation.eventsession;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class EditEventSessionRequestTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EditEventSessionRequest validRequest() {
        EditEventSessionRequest req = new EditEventSessionRequest();

        req.setEventSessionId(UUID.randomUUID());
        req.setUpdateAction(EUpdateAction.ADD);

        OffsetDateTime start = OffsetDateTime.now().plusDays(15).withHour(8);
        OffsetDateTime end = start.plusHours(2);

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        req.setExpectedVolAmount(10);
        req.setExpectedSerAmount(10);

        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        var violations = validator.validate(validRequest());
        assertThat(violations).isEmpty();
    }

    // -------- updateAction --------
    @Test
    void should_fail_when_updateAction_null() {
        var req = validRequest();
        req.setUpdateAction(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // -------- startDateTime --------
    @Test
    void should_fail_when_start_in_past() {
        var req = validRequest();
        req.setStartDateTime(OffsetDateTime.now().minusDays(1));

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_TIME_RANGE");
    }

    // -------- endDateTime --------
    @Test
    void should_fail_when_end_in_past() {
        var req = validRequest();
        req.setEndDateTime(OffsetDateTime.now().minusDays(1));

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_START_END_TIME");
    }

    // -------- expectedVolAmount --------
    @Test
    void should_fail_when_expectedVol_null() {
        var req = validRequest();
        req.setExpectedVolAmount(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_EXPECTED_VOL_AMOUNT");
    }

    @Test
    void should_fail_when_expectedVol_negative() {
        var req = validRequest();
        req.setExpectedVolAmount(-1);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_EXPECTED_VOL_AMOUNT");
    }

    @Test
    void should_fail_when_expectedVol_too_large() {
        var req = validRequest();
        req.setExpectedVolAmount(10000001);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_EXPECTED_VOL_AMOUNT");
    }

    // -------- expectedSerAmount --------
    @Test
    void should_fail_when_expectedSer_null() {
        var req = validRequest();
        req.setExpectedSerAmount(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_EXPECTED_SER_AMOUNT");
    }

    @Test
    void should_fail_when_expectedSer_negative() {
        var req = validRequest();
        req.setExpectedSerAmount(-1);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_EXPECTED_SER_AMOUNT");
    }

    // ================== EventSessionTime ==================

    @Test
    void should_fail_when_start_after_end() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1).withHour(10);
        OffsetDateTime end = start.minusHours(1);

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_START_END_TIME");
    }

    @Test
    void should_fail_when_cross_day() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(15).withHour(22);
        OffsetDateTime end = start.plusHours(3); // sang ngày hôm sau

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_TIME_RANGE");
    }

    @Test
    void should_fail_when_start_before_5am() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(15).withHour(4);
        OffsetDateTime end = start.plusHours(2);

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_START_TIME");
    }

    @Test
    void should_fail_when_end_after_23h() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(15).withHour(22);
        OffsetDateTime end = start.plusHours(2); // 24h+

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_TIME_RANGE");
    }

    @Test
    void should_fail_when_duration_less_than_1h() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1).withHour(8);
        OffsetDateTime end = start.plusMinutes(30);

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_TIME_RANGE");
    }

    @Test
    void should_fail_when_duration_more_than_12h() {
        var req = validRequest();
        OffsetDateTime start = OffsetDateTime.now().plusDays(1).withHour(6);
        OffsetDateTime end = start.plusHours(13);

        req.setStartDateTime(start);
        req.setEndDateTime(end);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SESSION_TIME_RANGE");
    }

    // -------- multiple --------
    @Test
    void should_fail_when_multiple_fields_invalid() {
        var req = validRequest();
        req.setUpdateAction(null);
        req.setExpectedVolAmount(-1);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(2);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "MISSING_REQUIRED_FIELD",
                        "INVALID_EVENT_EXPECTED_VOL_AMOUNT"
                );
    }
}
