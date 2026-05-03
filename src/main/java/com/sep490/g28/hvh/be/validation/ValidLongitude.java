package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.LatitudeValidator;
import com.sep490.g28.hvh.be.validation.validator.LongitudeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation used to validate a Double as Longitude value
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
 * @NotNull(message = "INVALID_LONGITUDE")
 * @ValidLongitude
 * Double checkInPlaceLng;
 * }
 * </pre>
 *
 * <p>The {@code message} attribute can be used to customize
 * the validation error message.</p>
 */
@Documented
@Constraint(validatedBy = LongitudeValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLongitude {

    String message() default "INVALID_LONGITUDE"; // Default error message
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
