package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import com.sep490.g28.hvh.be.validation.AtLeastOneFieldNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class AtLeastOneFieldNotNullValidator implements ConstraintValidator<AtLeastOneFieldNotNull, Object> {

    private Set<String> targetFields;

    @Override
    public void initialize(AtLeastOneFieldNotNull constraintAnnotation) {
        targetFields = new HashSet<>(Arrays.asList(constraintAnnotation.fields()));
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        Field[] fields = value.getClass().getDeclaredFields();

        try {
            for (Field field : fields) {

                // nếu có config fields -> chỉ check những field đó
                if (!targetFields.isEmpty() && !targetFields.contains(field.getName())) {
                    continue;
                }

                field.setAccessible(true);
                Object fieldValue = field.get(value);

                switch (fieldValue) {
                    case null -> {
                        continue;
                    }

                    // String not blank
                    case String str -> {
                        if (!str.isBlank()) return true;
                    }
                    //Collection not empty
                    case Collection<?> col -> {
                        if (!col.isEmpty()) return true;
                    }

                    // Other types
                    default -> {
                        return true;
                    }
                }

            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(ValidationErrorCode.AT_LEAST_ONE_FIELD_REQUIRED.name())
                .addPropertyNode("request")
                .addConstraintViolation();


        return false;
    }
}
