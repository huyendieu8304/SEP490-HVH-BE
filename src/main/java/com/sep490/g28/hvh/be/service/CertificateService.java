package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.certificate.response.VerifyCertificateResponse;
import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Volunteer;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CertificateService {
    void generateCertificate(Volunteer volunteer, Event event);

    void generateCertificates(List<Volunteer> volunteers, Event event);

    Page<VolunteerCertificateResponse> getCertificatesByVolunteer(int pageNumber, int pageSize, String eventName);

    VerifyCertificateResponse verifyCertificate(String certCode);


//    VerifyCertificateResponse verify(String code);

//    List<CertificateResponse> getByUser(UUID userId);

//    void revoke(UUID id);
}
