package com.sep490.g28.hvh.be.dto.certificate.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificateContentPayload {
    String organizationName;
    String volunteerFullName;
    String vid;
    String eventName;
    String hostFullName;
    String issuedDate;
    String verifyUrl;
    String qrBase64;
}
