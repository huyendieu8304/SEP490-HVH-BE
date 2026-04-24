package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error code for communicating with Supabase
 * Code format: 9xxx
 */
@Getter
@AllArgsConstructor
public enum SupabaseErrorCode implements ErrorCode {

    //this will contain the exception mostly for developer to read
    VALIDATION_FAIL(9001, "Data gửi cho supabase sai, kiểm tra lại log và code", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(9002, "Service role để gửi request tới supabse sai, kiểm tra lại api secret key", HttpStatus.INTERNAL_SERVER_ERROR),
    RATE_LIMIT_EXCEEDED(9003, "Vuợt quá rate limit gửi request tới supabase", HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_SERVER_ERROR(9004, "Supabase has internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(9005, "Supabase không xác thực được request", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_UPLOAD_FAIL(9006, "Lỗi xảy ra khi upload file lên Storage", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_FILE_NOT_EXISTED(9007, "File không tồn tại trên hệ thống lưu trữ", HttpStatus.BAD_REQUEST),
    STORAGE_GET_SIGNED_URL_FAIL(9008, "Có lỗi xảy ra khi lấy URL truy cập file", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_GET_UPLOAD_URL_FAIL(9010, "Có lỗi xảy ra khi lấy URL upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    STORAGE_DELETE_FILE_FAIL(9011, "Có lỗi xảy ra khi xóa file", HttpStatus.INTERNAL_SERVER_ERROR),
    AUTH_CREATE_ACCOUNT_FAIL(9012, "Có lỗi xảy ra khi tạo tài khoản", HttpStatus.INTERNAL_SERVER_ERROR),
    METHOD_NOT_ALLOWED(9013, "Sai method gửi request tới Supabase", HttpStatus.METHOD_NOT_ALLOWED),
    AUTH_EMAIL_USED(9014, "Email đã được sử dụng bởi một người khác", HttpStatus.INTERNAL_SERVER_ERROR),
    AUTH_CHANGE_PASSWORD_FAIL(9015, "Có lỗi xảy ra khi thay đổi mật khẩu tài khoản", HttpStatus.INTERNAL_SERVER_ERROR),
    AUTH_ACCOUNT_NOT_EXISTED(9016, "Tài khoản không tồn tại", HttpStatus.BAD_REQUEST),
    AUTH_CONFIRM_OLD_PASSWORD_FAIL(9017, "Có lỗi xảy ra khi xác nhận mật khẩu tài khoản", HttpStatus.BAD_REQUEST),
    AUTH_CHANGE_PHONE_FAIL(9018, "Có lỗi xảy ra khi thay đổi số điện thoại của tài khoản", HttpStatus.BAD_REQUEST),
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
