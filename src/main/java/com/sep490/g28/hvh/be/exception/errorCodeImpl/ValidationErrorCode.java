package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Contain Error code which relate to data validation
 * Code in format 2xxx
 */
@Getter
@AllArgsConstructor
public enum ValidationErrorCode implements ErrorCode {

    VALIDATION_ERROR(4000, "Error occur during validation data", HttpStatus.BAD_REQUEST),
    //from 2000
    INVALID_ERROR_CODE(2000, "Might have some spelling mistake in validation", HttpStatus.I_AM_A_TEAPOT),
    INVALID_DATA_TYPE(2001, "Invalid data type", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_FORMAT(2002, "Invalid request format. Please check your input, there might be one field with wrong format.", HttpStatus.BAD_REQUEST),
    MISSING_QUERY_PARAM(2003, "Missing required parameter. ", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(2004, "Địa chỉ email không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(2005, "Mật khẩu phải có ít nhất 8 kí tự, trong đó có ít nhất 1 chữ cái, 1 chữ số, 1 kí tự đặc biệt (!@#$%^&*.,:;’)", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(2006, "Số điện thoại phải là số di động hợp lệ ở Việt Nam", HttpStatus.BAD_REQUEST),
    INVALID_CID(2007, "Số căn cước công dân không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_FILE_SIZE_MAX(2008, "{fieldName} phải có kích thước nhỏ hơn {maxFileSizeMb}Mb.", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(2009, "{fieldName} phải là định dạng .jpg/.jpeg,.png hoặc .pdf.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_TYPE(2010, "{fieldName} phải là định dạng .jpg/.jpeg hoặc .png.", HttpStatus.BAD_REQUEST),
    INVALID_OTP(2011, "Mã OTP là chuỗi 6 kí tự chữ số", HttpStatus.BAD_REQUEST),
    INVALID_ORG_TYPE(2012, "Loại tổ chức không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_VOLUNTEER_VERIFICATION_STATUS(2013, "Trạng thái truyền vào không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_NUMBER(2014, "Số trang phải là số nguyên >=0", HttpStatus.BAD_REQUEST),
    INVALID_PAGE_SIZE(2015, "Số lượng bản ghi trong một trang là số nguyên và giới hạn từ 1 tới 100", HttpStatus.BAD_REQUEST),
    INVALID_UUID(2016, "Định dạng ID không đúng", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD(2017, "{fieldName} không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_REJECTION_REASON(2018, "Lí do từ chối không được để trống", HttpStatus.BAD_REQUEST),
    INVALID_ORGANIZATION_REGISTRATION_STATUS(2019, "Trạng thái đơn đăng ký tổ chức truyền vào không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_FULL_NAME(2020, "Họ tên đầy đủ không hợp lệ (Viết hoa chữ cái đầu, không được chứa số hay kí tự đặc biệt, chỉ được 1 dấu cách giữa các từ).", HttpStatus.BAD_REQUEST),
    INVALID_DATE_OF_BIRTH(2021, "Tuổi của bạn phải từ {min} tới {max} tuổi.", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(2022, "Địa chỉ không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_DETAIL_ADDRESS(2023, "Địa chỉ chi tiết không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_SUBDOMAIN_UPDATE(2024, "Dữ liệu cập nhật lĩnh vực tình nguyện con không hợp lệ" +
            ", cần tuân thủ theo: EDIT: id + name, DELETE: id, ADD: name", HttpStatus.BAD_REQUEST),
    INVALID_STRING_LENGTH(2025, "Độ dài chuỗi ký tự không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_UPDATE_ACTION(2026, "Phải có hành động update khi gửi yêu cầu (ADD/ EDIT/ REMOVE)", HttpStatus.BAD_REQUEST),
    INVALID_LATITUDE(2027, "Vĩ độ không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_LONGITUDE(2028, "Kinh độ không hợp lệ", HttpStatus.BAD_REQUEST),

    INVALID_EVENT_AUTO_APPROVE(2029, "Sự kiện phải được phân loại là tự động phê duyệt hoặc không", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SUBDOMAIN_ID(2030, "Sự kiện phải thuộc về 1 lĩnh vự hoạt động", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_EXPECTED_VOL_AMOUNT(2031, "Số lượng tình nguyện viên dự kiến không hợp lệ (>=0 và < 10000000)", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_EXPECTED_SER_AMOUNT(2032, "Số người được phục vụ dự kiến không hợp lệ (>=0 và < 10000000)", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_DATE_TIME(2033, "Ngày và thời gian tổ chức sự kiện không hợp lệ.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_TIME_RANGE(2034, "Thời gian bắt đầu và thời gian kết thúc phải trong 1 ngày và cách nhau tối thiểu 1 tiếng, tối đa 12 tiếng.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_START_END_TIME(2035, "Thời gian kết thúc phải sau thời gian bắt đầu.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_START_TIME(2036, "Thời gian bắt đầu không được sớm hơn 5 giờ sáng.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_END_TIME(2037, "Thời gian kết thúc không được muộn hơn 23 giờ.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_CHECKIN_ACCURACY_RANGE(2038, "Phạm vi check in phải lớn hơn 300m, và nhỏ hơn 3000m", HttpStatus.BAD_REQUEST),
    INVALID_NOTIFICATION_TOKEN(2039, "Token để nhận thông báo không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_DEVICE_ID(2040, "ID thiết bị không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_STATUS(2041, "Trạng thái sự kiện không tồn tại", HttpStatus.BAD_REQUEST ),
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }

    public String formatMessage(Map<String, Object> params) {
        String result = this.getMessage();
        for (var e : params.entrySet()) {
            result = result.replace(
                    "{" + e.getKey() + "}",
                    String.valueOf(e.getValue())
            );
        }
        return result;
    }
}
