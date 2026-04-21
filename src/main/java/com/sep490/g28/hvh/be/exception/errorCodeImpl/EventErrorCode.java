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
    INVALID_EVENT_RECRUITMENT_END_DATE(7007, "Ngày kết thúc tuyển người phải trước ngày đầu tiên của sự kiện ít nhất 3 ngày.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_START_DATE(7008, "Ngày bắt đầu tổ chức sự kiện phải cách ngày hôm nay ít nhất 15 ngày.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_END_DATE(7009, "Ngày cuối cùng của sự kiện phải sau ngày đầu tiên của sự kiện.", HttpStatus.BAD_REQUEST),
    INVALID_EVENT_SESSION_TIME_RANGE(7010, "Khoảng cách giữa thời gian bắt đầu và kết thúc sự kiện trong 1 ngày phải nằm trong khoảng cho phép của lĩnh vực hoạt động.", HttpStatus.BAD_REQUEST),
    ACTION_NOT_EXECUTABLE(7011, "Trạng thái của sự kiện không cho phép bạn thực hiện hành động này.", HttpStatus.BAD_REQUEST),
    DUPLICATE_HOSTED_DATE(7012, "Ngày tổ chức sự kiện này trùng với ngày host tổ chức một sự kiện khác.", HttpStatus.BAD_REQUEST),

    EVENT_SESSION_NOT_EXISTED(7013, "Event session not found", HttpStatus.NOT_FOUND),
    ALREADY_APPLIED(7014, "Tình nguyện viên đã đăng kí tham gia buổi tình nguyện này của sự kiện.", HttpStatus.CONFLICT),
    EVENT_SESSION_FULL(7015, "Buổi tình nguyện đã đủ số lượng tình nguyện viên đăng kí tham gia.", HttpStatus.CONFLICT),
    EVENT_RECRUITMENT_CLOSED(7016, "Sự kiện đã ngừng tiếp nhận đơn đăng kí.", HttpStatus.CONFLICT),
    APPLYING_SESSION_TIME_CONFLICT(7017, "Buổi tình nguyện này đã trùng với thời gian của một buổi khác mà bạn đã đăng kí trước đó", HttpStatus.CONFLICT),
    EVENT_NOT_RECRUITING(7018, "Sự kiện đang không trong trạng thái tiếp nhận đơn đăng kí.", HttpStatus.CONFLICT ),

    EVENT_APPLICATION_NOT_EXISTED(7019, "Event application not found", HttpStatus.NOT_FOUND),
    EVENT_APPLICATION_NOT_PENDING(7020, "Đơn đăng kí không ở trong trạng thái chờ phê duyệt", HttpStatus.CONFLICT),
    EVENT_APPLICATION_CANNOT_CANCELLED(7021, "Đơn đăng kí đang ở trong trạng thái không cho phép hủy", HttpStatus.CONFLICT),

    EVENT_ANNOUNCEMENT_CANNOT_SENT(7022, "Trạng thái của sự kiện không cho phép host gửi thông báo", HttpStatus.CONFLICT),

    EVENT_CANNOT_CANCELLED(7023, "Sự kiện đang ở trong trạng thái không cho phép hủy", HttpStatus.CONFLICT),
    EVENT_CANNOT_UPDATED(7024, "Sự kiện đang ở trong trạng thái không cho phép cập nhật thông tin", HttpStatus.CONFLICT),

    EVENT_SESSION_NOT_STARTED(7025, "Phiên sự kiện chưa diễn ra.", HttpStatus.CONFLICT),
    EVENT_NOT_ONGOING(7026, "Sự kiện đang không trong trạng thái diễn ra.", HttpStatus.CONFLICT),
    EVENT_CHECK_IN_CODE_NOT_MATCH(7027, "Mã điểm danh sự kiện không chính xác.", HttpStatus.CONFLICT),
    ALREADY_CHECKED_IN(7028, "Đã điểm danh vào sự kiện này.", HttpStatus.CONFLICT),
    EVENT_CHECK_IN_OUT_OF_RANGE(7029, "Ngoài phạm vi điểm danh sự kiện.", HttpStatus.CONFLICT),
    DEVICE_ALREADY_CHECKED_IN(7030, "Không thể điểm danh nhanh trên thiết bị này.", HttpStatus.CONFLICT),
    EVENT_SESSION_ENDED(7031, "Phiên sự kiện đã kết thúc.", HttpStatus.CONFLICT),

    NO_CHANGES_IN_UPDATE_REQUEST(7032, "Yêu cầu cập nhật không chứa thay đổi nào", HttpStatus.BAD_REQUEST),
    EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE(7033, "Không thể phê duyệt sự kiện do sự kiện đã quá hạn tuyển người", HttpStatus.CONFLICT),
    EVENT_APPLICATION_CANNOT_PROCESS(7034, "Sự kiện đang ở trong trạng thái không thể phê duyệt đơn đăng kí.", HttpStatus.CONFLICT),
    EVENT_SESSION_NOT_CHECKED_IN(7035, "Chưa điểm danh trong sự kiện hiện tại.", HttpStatus.CONFLICT),
    DEVICE_NOT_CHECKED_IN(7036, "Thiết bị chưa được sử dụng để điểm danh.", HttpStatus.CONFLICT),

    EVENT_CANNOT_ASSIGNED_HOST(7037, "Sự kiện đang ở trong trạng thái không cho phép phân công host.", HttpStatus.CONFLICT),
    EVENT_CANNOT_ASSIGNED_TO_INACTIVE_HOST(7038, "Không thể phân công sự kiện cho tài khoản host đang bị khóa.", HttpStatus.CONFLICT),
    EVENT_CANNOT_ASSIGN_TO_HOST_NOT_IN_ORGANIZATION(7039, "Không thể phân công sự kiện cho host không thuộc tổ chức.", HttpStatus.CONFLICT),
    EVENT_CANNOT_ASSIGNED_TO_CURRENT_HOST(7040, "Không thể phân công sự kiện cho tài khoản host hiện đang phụ trách.", HttpStatus.CONFLICT),

    EVENT_CLAIM_OUT_OF_CLAIM_TIME(7041, "Đã hết thời gian khiếu nại. ", HttpStatus.CONFLICT),
    EVENT_SESSION_ALREADY_CLAIMED(7042, "Đã khiếu nại cho phiên sự kiện này. ", HttpStatus.CONFLICT),
    EVENT_CLAIM_NOT_FOUND(7043, "Không tìm thấy khiếu nại. ", HttpStatus.NOT_FOUND),
    EVENT_CLAIM_ALREADY_RESOLVED(7044, "Khiếu nại đã được xử lý. ", HttpStatus.CONFLICT),
    EVENT_CLAIM_OUT_OF_VERIFY_TIME(7045, "Đã hết thời gian xử lý khiếu nại này. ", HttpStatus.CONFLICT),

    EVENT_MOMENT_ALREADY_SHARED(7046, "Đã chia sẻ khoảnh khắc trong phiên sự kiện này.", HttpStatus.CONFLICT),
    EVENT_MOMENT_NOT_FOUND(7047, "Không tìm thấy khoảnh khắc.", HttpStatus.NOT_FOUND),
    EVENT_CLAIM_INVALID_HONOR_HOUR_REQUEST(7048, "Số giờ cần bổ sung không được vượt quá số giờ tín nhiệm thực tế", HttpStatus.NOT_FOUND),
    EVENT_CANNOT_DELETED(7037, "Sự kiện đang ở trong trạng thái không cho phép xóa.", HttpStatus.CONFLICT),

    ;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getName() {
        return this.name();
    }
}
