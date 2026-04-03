package com.sep490.g28.hvh.be.dto.host.response;

import com.sep490.g28.hvh.be.constant.EAccountStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostSimpleResponseForManager {
    UUID id;
    String fullName;
    String address;
    String email;
    String phone;
    EAccountStatus status;
    int hostedEventCount;

    public HostSimpleResponseForManager(
            UUID id,
            String fullName,
            String address,
            String email,
            String phone,
            EAccountStatus status,
            long hostedEventCount
    ) {
        this.id = id;
        this.fullName = fullName;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.hostedEventCount = (int) hostedEventCount;
    }
}
