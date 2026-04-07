package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.certificate.response.VerifyCertificateResponse;
import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CertificateService {
    String generate(UUID volId, UUID eventId);

    Page<VolunteerCertificateResponse> getCertificatesByVolunteer(int pageNumber, int pageSize, String eventName);

    VerifyCertificateResponse verifyCertificate(String certCode);


//    VerifyCertificateResponse verify(String code);

//    List<CertificateResponse> getByUser(UUID userId);

//    void revoke(UUID id);
}
