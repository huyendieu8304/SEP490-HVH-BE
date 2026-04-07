package com.sep490.g28.hvh.be.dto.certificate.response;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
public class VolunteerCertificateResponse {
    String eventName;
    String organizationName;
    String certCode;
    String certSignedUrl;
    OffsetDateTime issuedAt;

    public VolunteerCertificateResponse(
            String eventName,
            String organizationName,
            String certCode,
            String certSignedUrl,
            OffsetDateTime issuedAt
    ) {
        this.eventName = eventName;
        this.issuedAt = issuedAt;
        this.organizationName = organizationName;
        this.certCode = certCode;
        this.certSignedUrl = certSignedUrl;
    }
}
