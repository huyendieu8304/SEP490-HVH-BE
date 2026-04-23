package com.sep490.g28.hvh.be.validation.eventimage;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public class EditEventImageRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private EditEventImageRequest validRequest() {
        EditEventImageRequest req = new EditEventImageRequest();
        req.setImageId(UUID.randomUUID());
        req.setUpdateAction(EUpdateAction.ADD);
        req.setFileExtension(".jpg");
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
        EditEventImageRequest req = validRequest();
        req.setUpdateAction(null);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_UPDATE_ACTION");
    }

    // -------- fileExtension --------
    @ParameterizedTest
    @ValueSource(strings = {
            ".pdf",
            ".txt",
            ".html",
            ".gif",
            "",
            "   "
    })
    void should_fail_when_file_extension_invalid(String ext) {
        EditEventImageRequest req = validRequest();
        req.setFileExtension(ext);

        var violations = validator.validate(req);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("INVALID_IMAGE_TYPE");
    }

    // -------- fileExtension null --------
    @Test
    void should_pass_when_file_extension_null() {
        EditEventImageRequest req = validRequest();
        req.setFileExtension(null);

        var violations = validator.validate(req);

        // hiện tại validator cho phép null
        assertThat(violations).isEmpty();
    }

    // -------- multiple --------
    @Test
    void should_fail_when_multiple_fields_invalid() {
        EditEventImageRequest req = validRequest();
        req.setUpdateAction(null);
        req.setFileExtension(".txt");

        var violations = validator.validate(req);

        assertThat(violations).hasSize(2);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "INVALID_UPDATE_ACTION",
                        "INVALID_IMAGE_TYPE"
                );
    }
}
