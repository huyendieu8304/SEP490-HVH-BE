package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.host.response.HostActivitiesResponseForManager;
import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import com.sep490.g28.hvh.be.entity.Host;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
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
                    COUNT(DISTINCT e.id),
                    h.avatarUrl
                )
                FROM Host h
                LEFT JOIN Event e
                    ON e.host.id = h.id
                    AND e.status = 'COMPLETED'
                LEFT JOIN User u ON u.id = h.id
                WHERE h.createdBy.id = :orgManagerId
                    AND (:email IS NULL OR h.email ILIKE CONCAT('%', CAST(:email AS string), '%'))
                GROUP BY h.id, h.fullName, h.address, h.email, h.phone, u.status
            """)
    Page<HostSimpleResponseForManager> getHostsByManager(UUID orgManagerId, Pageable pageable, String email);

    boolean existsByIdAndCreatedBy_Id(UUID hostId, UUID orgManagerId);

    @Query("""
                SELECT new com.sep490.g28.hvh.be.dto.host.response.HostActivitiesResponseForManager(
                    e.id,
                    e.name,
                    e.address,
                    e.detailAddress,
                    e.status,
                    s.id,
                    s.startDateTime,
                    s.endDateTime
                )
                FROM Event e
                LEFT JOIN EventSession s
                    ON s.event.id = e.id
                WHERE e.host.id = :hostId
                    AND s.startDateTime BETWEEN :from AND :to
                ORDER BY e.createdAt DESC
            """)
    Page<HostActivitiesResponseForManager> getHostActivitiesByManager(UUID hostId, Pageable pageable, OffsetDateTime from, OffsetDateTime to);
}
