package com.sep490.g28.hvh.be.validation;

import com.sep490.g28.hvh.be.validation.validator.AtLeastOneFieldNotNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AtLeastOneFieldNotNullValidator.class)
public @interface AtLeastOneFieldNotNull {

    String message() default "AT_LEAST_ONE_FIELD_REQUIRED";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // optional: chỉ định field cần check (nếu không set -> check tất cả)
    String[] fields() default {};
}
