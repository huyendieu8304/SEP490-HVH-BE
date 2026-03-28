package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.LatLngValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation used to validate latitude and longitude must have both or nothing
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
 * @ValidLatLng
 * public class UpdateEventRequest {}
 * }
 * </pre>
 *
 * <p>The {@code message} attribute can be used to customize
 * the validation error message.</p>
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LatLngValidator.class)
public @interface ValidLatLng {
    String message() default "INVALID_LAT_LNG";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
