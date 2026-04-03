package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RateAndReviewErrorCode implements ErrorCode {
    RATE_EVENT_NOT_IN_ALLOWED_TIME(8001, "Chưa đến hoặc đã qua thời gian cho phép đánh giá sự kiện.", HttpStatus.CONFLICT),
    NOT_RECORDED_AS_PARTICIPANT(8002, "Hệ thống chưa ghi nhận người dùng đã tham gia sự kiện, không thể đánh giá.", HttpStatus.CONFLICT),
    ALREADY_RATED_EVENT(8003, "Hệ thống ghi nhận đã tồn tại đánh giá của tình nguyện viên đối với sự kiện này.", HttpStatus.CONFLICT),

    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
