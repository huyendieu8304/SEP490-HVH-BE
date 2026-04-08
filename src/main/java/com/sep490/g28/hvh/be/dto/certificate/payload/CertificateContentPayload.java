package com.sep490.g28.hvh.be.dto.certificate.payload;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CertificateContentPayload {
    String organizationName;
    String volunteerFullName;
    String vid;
    String eventName;
    String hostFullName;
    LocalDate issuedDate;
    String verifyUrl;
    String qrBase64;
}
