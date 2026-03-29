package com.sep490.g28.hvh.be.validation.validator;

import com.sep490.g28.hvh.be.dto.eventsession.request.EditEventSessionRequest;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ValidationErrorCode;
import com.sep490.g28.hvh.be.validation.EventSessionTime;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.*;

public class EventSessionTimeValidator implements ConstraintValidator<EventSessionTime, Object> {

    @Override
    public boolean isValid(Object o, ConstraintValidatorContext context) {

        //earliest start time = 5h
        //latest end time = 23h
        //session max time in a day= 12
        //session min time = 1 hour
        // in one day

        String endDateTimeFieldName = "endDateTime";
        String startDateTimeFieldName = "startDateTime";

        OffsetDateTime start;
        OffsetDateTime end;


        if (o instanceof EditEventSessionRequest r) {
            start = r.getStartDateTime();
            end = r.getEndDateTime();

            if (!start.isBefore(end)) {
                invalid(context, endDateTimeFieldName,
                        ValidationErrorCode.INVALID_EVENT_SESSION_START_END_TIME);
                return false;
            }

            //get date only
            LocalDate startDay = start.toLocalDate();
            LocalDate endDay = end.toLocalDate();

            // session must be in 1 day
            if (!startDay.equals(endDay)) {
                invalid(context, endDateTimeFieldName,
                        ValidationErrorCode.INVALID_EVENT_SESSION_TIME_RANGE);
                return false;
            }

            //get time only (VN hour)
            LocalTime startTime = start.toLocalTime();
            LocalTime endTime = end.toLocalTime();

            // not start before 05:00
            if (startTime.isBefore(LocalTime.of(5, 0))) {

                invalid(context, startDateTimeFieldName,
                                ValidationErrorCode.INVALID_EVENT_SESSION_START_TIME);
                return false;

            }

            // not end after 23:00
            if (endTime.isAfter(LocalTime.of(23, 0))) {
                invalid(context, endDateTimeFieldName,
                        ValidationErrorCode.INVALID_EVENT_SESSION_END_TIME);
                return false;
            }

            Duration duration = Duration.between(start, end);

            // min session 1h
            if (duration.compareTo(Duration.ofHours(1)) < 0) {
                invalid(context, endDateTimeFieldName,
                        ValidationErrorCode.INVALID_EVENT_SESSION_TIME_RANGE);
                return false;
            }

            // max session 12h
            if (duration.compareTo(Duration.ofHours(12)) > 0) {
                invalid(context, endDateTimeFieldName,
                        ValidationErrorCode.INVALID_EVENT_SESSION_TIME_RANGE);
                return false;
            }
        }

        return true;
    }

    //kinda throw exception :vv
    private void invalid(
            ConstraintValidatorContext context,
            String field,
            ValidationErrorCode errorCode
    ) {
        context.disableDefaultConstraintViolation();

        context.buildConstraintViolationWithTemplate(errorCode.name())
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
