package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import com.sep490.g28.hvh.be.entity.Host;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface HostRepository extends JpaRepository<Host, UUID> {

    Long countHostByOrganizationId(UUID orgId);

    @Query("""
                SELECT new com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager(
                    h.id,
                    h.fullName,
                    h.address,
                    h.email,
                    h.phone,
                    u.status,
                    COUNT(DISTINCT e.id)
                )
                FROM Host h
                LEFT JOIN Event e
                    ON e.host.id = h.id
                    AND e.status = 'COMPLETED'
                    AND (:email IS NULL OR h.email ILIKE CONCAT('%', CAST(:email AS string), '%'))
                LEFT JOIN User u ON u.id = h.id
                WHERE h.createdBy.id = :orgManagerId
                GROUP BY h.id, h.fullName, h.address, h.email, h.phone
            """)
    Page<HostSimpleResponseForManager> getHostsByManager(UUID orgManagerId, Pageable pageable, String email);

}
