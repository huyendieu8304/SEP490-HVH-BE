package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Contain Error code related to Volunteer business rules
 * Code in format 3xxx
 */
@Getter
@AllArgsConstructor
public enum VolunteerErrorCode implements ErrorCode {

    CID_USED(3001, "Số căn cước công dân đã được sử dụng bởi một tình nguyện viên khác.", HttpStatus.BAD_REQUEST),
    EMAIL_USED(3002, "Email đã được sử dụng bởi một tình nguyện viên khác.", HttpStatus.BAD_REQUEST),
    PHONE_USED(3003, "Số điện thoại đã được sử dụng bởi một tình nguyện viên khác.", HttpStatus.BAD_REQUEST),
    NICKNAME_USED(3004, "Nickname đã được sử dụng bởi một tình nguyện viên khác.", HttpStatus.BAD_REQUEST),
    REGISTRATION_NOT_EXISTED(3005, "Đơn đăng kí tình nguyện viên không tồn tại.", HttpStatus.NOT_FOUND),
    REGISTRATION_VERIFIED(3006, "Đơn đăng kí tình nguyện viên đã được xác thực.", HttpStatus.BAD_REQUEST),
    VOLUNTEER_NOT_EXISTED(3007, "Tình nguyện viên không tồn tại", HttpStatus.NOT_FOUND)
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
