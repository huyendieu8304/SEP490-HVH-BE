package com.sep490.g28.hvh.be.dto.eventclaim.request;

import com.sep490.g28.hvh.be.validation.AllowedFileExtension;
import com.sep490.g28.hvh.be.validation.RequiredField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClaimEventHourRequest {
    @NotBlank(message = "INVALID_UUID")
    @UUID(message = "INVALID_UUID")
    String eventSessionId;

    @RequiredField(fieldName = "Số giờ mong muốn bổ sung")
    @Min(value = 4)
    @Max(value = 12)
    Short honorHours;

    @RequiredField(fieldName = "Lý do khiếu nại")
    @Length(max = 100, message = "INVALID_STRING_LENGTH")
    String reason;

    @RequiredField(fieldName = "Chi tiết lý do khiếu nại")
    @Length(max = 300, message = "INVALID_STRING_LENGTH")
    String detailReason;

    @NotBlank(message = "INVALID_FILE_TYPE")
    @AllowedFileExtension(fieldName = "Bằng chứng xác thực khiếu nại")
    String evidences;
}
