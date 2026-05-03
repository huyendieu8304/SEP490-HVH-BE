package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.LatitudeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation used to validate a Double as Latitude value
 *
 *
 * <p><b>Null handling:</b><br>
 * This annotation <b>allows {@code null}</b> values by design.
 * If the field is mandatory, it must be combined with
 * {@link jakarta.validation.constraints.NotNull}.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @NotNull(message = "INVALID_LATITUDE")
 * @ValidLatitude
 * Double checkInPlaceLat;
 * }
 * </pre>
 *
 * <p>The {@code message} attribute can be used to customize
 * the validation error message.</p>
 */
@Documented
@Constraint(validatedBy = LatitudeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLatitude {

    String message() default "INVALID_LATITUDE"; // Default error message
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
