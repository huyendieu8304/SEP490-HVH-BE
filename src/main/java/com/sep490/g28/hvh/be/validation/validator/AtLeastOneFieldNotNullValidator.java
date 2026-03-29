package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import com.sep490.g28.hvh.be.validation.AtLeastOneFieldNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;
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

                if (fieldValue == null) continue;

                // xử lý String
                if (fieldValue instanceof String str) {
                    if (!str.isBlank()) return true;
                } else {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            //todo xử  lí
            throw new RuntimeException(e);
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(ValidationErrorCode.AT_LEAST_ONE_FIELD_REQUIRED.name())
                .addConstraintViolation();


        return false;
    }
}
