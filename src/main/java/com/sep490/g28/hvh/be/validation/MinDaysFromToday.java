package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.MinDaysFromTodayValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MinDaysFromTodayValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface MinDaysFromToday {

    String message() default "Date must be at least {days} days from now";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int days(); // số ngày tối thiểu
}
