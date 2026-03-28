package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.dto.event.request.UpdateEventRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import com.sep490.g28.hvh.be.validation.ValidLatLng;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LatLngValidator implements ConstraintValidator<ValidLatLng, UpdateEventRequest> {
    @Override
    public boolean isValid(UpdateEventRequest request, ConstraintValidatorContext context) {
        if (request == null) return true;

        Double lat = request.getCheckInLocationLat();
        Double lng = request.getCheckInLocationLng();

        // cả 2 null -> OK
        if (lat == null && lng == null) return true;

        // 1 trong 2 null ->FAIL
        if (lat == null || lng == null) {
            invalid(context, "checkInLocationLat");
            invalid(context, "checkInLocationLng");
            return false;
        }
        return true;
    }

    //kinda throw exception :vv
    private void invalid(
            ConstraintValidatorContext context,
            String field
    ) {
        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(ValidationErrorCode.INVALID_LAT_LNG.name())
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
