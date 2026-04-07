package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import com.sep490.g28.hvh.be.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    @Query("""
                    SELECT new com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse(
                        e.name,
                        o.name,
                        c.code,
                        c.certificatePath,
                        c.issuedAt
                    ) FROM Certificate c
                    LEFT JOIN c.event e
                    LEFT JOIN e.organization o
                    WHERE c.volunteer.id = :volunteerId 
                        AND c.status = com.sep490.g28.hvh.be.constant.ECertificateStatus.ACTIVE
                        AND (:eventName IS NULL OR e.name ILIKE CONCAT('%', CAST(:eventName AS string), '%'))
                    ORDER BY c.issuedAt DESC            
            """)
    Page<VolunteerCertificateResponse> findByVolunteerIdAndEventName(Pageable pageable, UUID id, String eventName);

    Optional<Certificate> findByCode(String code);
}
