package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.validation.EventStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventStatusValidator implements ConstraintValidator<EventStatus, String> {
    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        //allow null
        if (s == null || s.isBlank()) return true;

        // Ensure the payment type exists within the enum
        try {
            EEventStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }
}
