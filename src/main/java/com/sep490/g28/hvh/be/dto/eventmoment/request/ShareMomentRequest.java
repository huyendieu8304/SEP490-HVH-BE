package com.sep490.g28.hvh.be.dto.eventmoment.request;

import com.sep490.g28.hvh.be.validation.AllowedFileExtension;
import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import com.sep490.g28.hvh.be.validation.RequiredField;
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
public class ShareMomentRequest {
    @NotBlank(message = "INVALID_UUID")
    @UUID(message = "INVALID_UUID")
    String eventSessionId;

    @RequiredField(fieldName = "Nội dung khoảnh khắc")
    @Length(max = 500, message = "INVALID_STRING_LENGTH")
    String momentContent;

    @NotBlank(message = "INVALID_FILE_TYPE")
    @AllowedFileExtension(fieldName = "Ảnh khoảnh khắc")
    String momentPictures;
}
