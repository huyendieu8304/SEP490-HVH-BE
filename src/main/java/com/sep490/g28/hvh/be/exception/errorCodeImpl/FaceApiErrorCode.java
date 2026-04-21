package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum FaceApiErrorCode implements ErrorCode {

    VALIDATION_FAIL(10001, "Server phản hồi lỗi, kiểm tra lại log và code", HttpStatus.INTERNAL_SERVER_ERROR),
    FACE_LIVENESS_CHECK_FAILED(10002, "Xác thực khuôn mặt thất bại, vui lòng làm theo hướng dẫn và thử lại", HttpStatus.BAD_REQUEST),
    FACE_RECOGNITION_FAILED(10003, "Không tìm thấy khuôn mặt của bạn, vui lòng thử lại", HttpStatus.NOT_FOUND),
    FACE_RECOGNITION_NOT_MATCH(10004, "Khuôn mặt được nhận diện không thuộc về tài khoản này, vui lòng thử lại", HttpStatus.CONFLICT),
    ALREADY_REGISTERED_FACE(10005, "Đã đăng ký khuôn mặt cho tài khoản này", HttpStatus.CONFLICT),
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
