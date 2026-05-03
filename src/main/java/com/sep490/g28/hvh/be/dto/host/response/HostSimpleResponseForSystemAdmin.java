package com.sep490.g28.hvh.be.dto.host.response;

import com.sep490.g28.hvh.be.constant.EAccountStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HostSimpleResponseForSystemAdmin {
    UUID id;
    String fullName;
    String address;
    String email;
    String phone;
    EAccountStatus status;
    int hostedEventCount;
    String avatarUrl;

    public HostSimpleResponseForSystemAdmin(
            UUID id,
            String fullName,
            String address,
            String email,
            String phone,
            EAccountStatus status,
            long hostedEventCount,
            String avatarUrl
    ) {
        this.id = id;
        this.fullName = fullName;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.hostedEventCount = (int) hostedEventCount;
        this.avatarUrl = avatarUrl;
    }
}
