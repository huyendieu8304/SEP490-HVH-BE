package com.sep490.g28.hvh.be.validation.event;


import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import com.sep490.g28.hvh.be.dictionary.WardDictionary;
import com.sep490.g28.hvh.be.dto.event.request.EditEventRequest;
import com.sep490.g28.hvh.be.validation.validator.WardValidator;
import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EditEventRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        WardDictionary wardDictionary = mock(WardDictionary.class);
        when(wardDictionary.contains(any())).thenReturn(true);

        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .constraintValidatorFactory(new ConstraintValidatorFactory() {

                    @Override
                    public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                        try {
                            if (key == WardValidator.class) {
                                return (T) new WardValidator(wardDictionary);
                            }
                            return key.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void releaseInstance(ConstraintValidator<?, ?> instance) {}
                })
                .buildValidatorFactory();

        validator = factory.getValidator();
    }

    private EditEventRequest validRequest() {
        EditEventRequest req = new EditEventRequest();
        req.setEventId(UUID.randomUUID());
        req.setName("Hỗ trợ người già");
        req.setDescription("Sự kiện chăm sóc và trò chuyện cùng người già neo đoen");
        req.setAddress("VALID_WARD"); // phải tồn tại trong WardDictionary
        req.setDetailAddress("Detail address");
        req.setAutoApprove(true);
        req.setServingActivity(true);
        req.setActivitySubDomainId((short) 1);
        req.setServedTarget(EServedTarget.ELDERLY);
        req.setServingPlaceType(EServingPlaceType.HOSPITAL);
        req.setRecruitmentEndDate(LocalDate.now().plusDays(5));
        req.setCheckInPlaceLat(10.0);
        req.setCheckInPlaceLng(106.0);
        req.setCheckInPlaceAccuracyMeters(500);
        return req;
    }

    @Test
    void should_pass_when_all_fields_valid() {
        Set<ConstraintViolation<EditEventRequest>> violations =
                validator.validate(validRequest());

        assertThat(violations).isEmpty();
    }

    // -------- name --------
    @Test
    void should_fail_when_name_null() {
        EditEventRequest req = validRequest();
        req.setName(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // -------- description --------
    @Test
    void should_fail_when_description_null() {
        EditEventRequest req = validRequest();
        req.setDescription(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // -------- address --------
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_fail_when_address_blank(String address) {
        EditEventRequest req = validRequest();
        req.setAddress(address);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_ADDRESS");
    }

    // -------- detailAddress --------
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void should_fail_when_detail_address_blank(String value) {
        EditEventRequest req = validRequest();
        req.setDetailAddress(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_DETAIL_ADDRESS");
    }

    @Test
    void should_fail_when_detail_address_too_long() {
        EditEventRequest req = validRequest();
        req.setDetailAddress("a".repeat(201));

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_DETAIL_ADDRESS");
    }

    // -------- boolean fields --------
    @Test
    void should_fail_when_autoApprove_null() {
        EditEventRequest req = validRequest();
        req.setAutoApprove(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_AUTO_APPROVE");
    }

    @Test
    void should_fail_when_servingActivity_null() {
        EditEventRequest req = validRequest();
        req.setServingActivity(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SERVING_ACTIVITY");
    }

    // -------- activitySubDomainId --------
    @Test
    void should_fail_when_subdomain_null() {
        EditEventRequest req = validRequest();
        req.setActivitySubDomainId(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SUBDOMAIN_ID");
    }

    @Test
    void should_fail_when_subdomain_negative() {
        EditEventRequest req = validRequest();
        req.setActivitySubDomainId((short) -1);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_SUBDOMAIN_ID");
    }

    // -------- enum --------
    @Test
    void should_fail_when_servedTarget_null() {
        EditEventRequest req = validRequest();
        req.setServedTarget(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    @Test
    void should_fail_when_servingPlaceType_null() {
        EditEventRequest req = validRequest();
        req.setServingPlaceType(null);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("MISSING_REQUIRED_FIELD");
    }

    // -------- recruitmentEndDate --------
    @Test
    void should_fail_when_recruitment_date_past() {
        EditEventRequest req = validRequest();
        req.setRecruitmentEndDate(LocalDate.now().minusDays(1));

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_RECRUITMENT_END_DATE");
    }

    @Test
    void should_fail_when_recruitment_date_less_than_min_days() {
        EditEventRequest req = validRequest();
        req.setRecruitmentEndDate(LocalDate.now().plusDays(1));

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_RECRUITMENT_END_DATE");
    }

    // -------- latitude --------
    @Test
    void should_fail_when_lat_invalid() {
        EditEventRequest req = validRequest();
        req.setCheckInPlaceLat(100.0);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_LATITUDE");
    }

    // -------- longitude --------
    @Test
    void should_fail_when_lng_invalid() {
        EditEventRequest req = validRequest();
        req.setCheckInPlaceLng(200.0);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_LONGITUDE");
    }

    // -------- accuracy --------
    @ParameterizedTest
    @ValueSource(ints = {100, 299, 3001})
    void should_fail_when_accuracy_invalid(int value) {
        EditEventRequest req = validRequest();
        req.setCheckInPlaceAccuracyMeters(value);

        var violations = validator.validate(req);

        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_EVENT_CHECKIN_ACCURACY_RANGE");
    }

    // -------- multiple --------
    @Test
    void should_fail_when_multiple_fields_invalid() {
        EditEventRequest req = validRequest();
        req.setName(null);
        req.setCheckInPlaceLat(100.0);
        req.setCheckInPlaceAccuracyMeters(100);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(3);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "MISSING_REQUIRED_FIELD",
                        "INVALID_LATITUDE",
                        "INVALID_EVENT_CHECKIN_ACCURACY_RANGE"
                );
    }
}
