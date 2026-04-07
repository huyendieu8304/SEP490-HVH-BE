package com.sep490.g28.hvh.be.service;

import java.util.UUID;

public interface CertificateService {
    String generate(UUID volId, UUID eventId);


//    VerifyCertificateResponse verify(String code);

//    List<CertificateResponse> getByUser(UUID userId);

//    void revoke(UUID id);
}
