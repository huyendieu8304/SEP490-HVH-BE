package com.sep490.g28.hvh.be.exception.errorCodeImpl;

import com.sep490.g28.hvh.be.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Contain Error code related to Event business rules
 * Code in format 7xxx
 */
@Getter
@AllArgsConstructor
public enum EventErrorCode implements ErrorCode {

    EVENT_NOT_EXISTED(7001, "Event not found", HttpStatus.NOT_FOUND),
    EVENT_NOT_EDITABLE(7002, "Sự kiện đang ở trong trạng thái không thể chỉnh sửa được", HttpStatus.CONFLICT),
    INVALID_IMAGES_AMOUNT(7003, "Số lượng ảnh giới hạn tối đa 5 ảnh", HttpStatus.BAD_REQUEST),
    INVALID_DATE_TIME_AMOUNT(7004, "Phải có ít nhất 1 ngày và thời gian diễn ra sự kiện", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_DATE_TIME_RANGE(7005, "Thời gian bắt đầu sự kiện phải trước thời gian kết thúc trong ngày", HttpStatus.BAD_REQUEST),
    DUPLICATE_SESSION_DAY(7006, "Trong 1 ngày chỉ có 1 buổi tình nguyện", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_RECRUITMENT_END_DATE(7007, "Ngày kết thúc tuyển người phải cách ngày hôm nay ít nhất 3 ngày và trước ngày bắt đầu sự kiện ít nhất 3 ngày.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_START_DATE(7008, "Ngày bắt đầu tổ chức sự kiện phải cách ngày hôm nay ít nhất 15 ngày.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_END_DATE(7009, "Ngày kết thúc sự kiện phải sau ngày bắt đầu sự kiện.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_TIME_RANGE(7010, "Khoảng cách giữa thời gian bắt đầu và kết thúc sự kiện trong 1 ngày phải nằm trong khoảng cho phép của lĩnh vực hoạt động.", HttpStatus.BAD_REQUEST),
    ACTION_NOT_EXECUTABLE(7011, "Trạng thái của sự kiện không cho phép bạn thực hiện hành động này.", HttpStatus.BAD_REQUEST),
    DUPLICATE_HOSTED_DATE(7012, "Trùng ngày tổ chức sự kiện của host với một sự kiện khác.", HttpStatus.BAD_REQUEST),

    EVENT_SESSION_NOT_EXISTED(7013, "Event session not found", HttpStatus.NOT_FOUND),
    ALREADY_APPLIED(7014, "Tình nguyện viên đã đăng kí tham gia buổi tình nguyện này của sự kiện.", HttpStatus.CONFLICT),
    EVENT_SESSION_FULL(7015, "Buổi tình nguyện đã đủ số lượng tình nguyện viên đăng kí tham gia.", HttpStatus.CONFLICT),
    EVENT_RECRUITMENT_CLOSED(7016, "Sự kiện đã ngừng tiếp nhận đơn đăng kí.", HttpStatus.CONFLICT),
    APPLYING_SESSION_TIME_CONFLICT(7017, "Buổi tình nguyện này đã trùng với thời gian của một buổi khác mà bạn đã đăng kí trước đó", HttpStatus.CONFLICT),
    EVENT_NOT_RECRUITING(7018, "Sự kiện đang không trong trạng thái tiếp nhận đơn đăng kí.", HttpStatus.CONFLICT ),

    EVENT_APPLICATION_NOT_EXISTED(7019, "Event application not found", HttpStatus.NOT_FOUND),
    EVENT_APPLICATION_NOT_PENDING(7020, "Đơn đăng kí không ở trong trạng thái chờ phê duyệt", HttpStatus.CONFLICT),
    EVENT_APPLICATION_CANNOT_CANCEL(7021, "Đơn đăng kí ở trong trạng thái không hủy được", HttpStatus.CONFLICT),

    EVENT_NOTIFICATION_CANNOT_SENT(7022, "Trạng thái của sự kiện không cho phép host gửi thông báo", HttpStatus.CONFLICT),
    EVENT_SESSION_NOT_STARTED(7025, "Phiên sự kiện chưa diễn ra.", HttpStatus.CONFLICT),
    EVENT_NOT_ONGOING(7026, "Sự kiện đang không trong trạng thái diễn ra.", HttpStatus.CONFLICT),
    EVENT_CHECK_IN_CODE_NOT_MATCH(7027, "Mã điểm danh sự kiện không chính xác.", HttpStatus.CONFLICT),
    ALREADY_CHECKED_IN(7028, "Đã điểm danh vào sự kiện này.", HttpStatus.CONFLICT),
    EVENT_CHECK_IN_OUT_OF_RANGE(7029, "Ngoài phạm vi điểm danh sự kiện.", HttpStatus.CONFLICT),
    DEVICE_ALREADY_CHECKED_IN(7030, "Thiết bị đã được sử dụng để điểm danh nhanh.", HttpStatus.CONFLICT),
    EVENT_SESSION_ENDED(7031, "Phiên sự kiện đã kết thúc.", HttpStatus.CONFLICT)
    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
