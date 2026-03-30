package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.validation.MinDaysFromToday;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class MinDaysFromTodayValidator implements ConstraintValidator<MinDaysFromToday, LocalDate> {

    private int days;

    @Override
    public void initialize(MinDaysFromToday constraintAnnotation) {
        this.days = constraintAnnotation.days();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) return true; // để @NotNull handle

        LocalDate minDate = LocalDate.now().plusDays(days);

        return !value.isBefore(minDate);
    }
}
