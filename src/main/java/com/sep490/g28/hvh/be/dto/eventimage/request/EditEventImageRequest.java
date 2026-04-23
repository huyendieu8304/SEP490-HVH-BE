package com.sep490.g28.hvh.be.dto.eventimage.request;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.validation.ImageFileExtension;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EditEventImageRequest {

    UUID imageId;

    @NotNull(message = "INVALID_UPDATE_ACTION") //thís can only ADD or REMOVE
    EUpdateAction updateAction;

    @ImageFileExtension(fieldName = "Ảnh sự kiện")
    String fileExtension;
}
