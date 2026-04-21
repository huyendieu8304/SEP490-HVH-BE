package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.EventStatusValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EventStatusValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventStatus {
    String message() default "INVALID_EVENT_STATUS";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
