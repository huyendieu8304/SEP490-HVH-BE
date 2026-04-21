package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.EventClaim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventClaimRepository extends JpaRepository<EventClaim, UUID> {
    EventClaim findByEventApplicationId(UUID eventApplicationId);

    @Query("""
            SELECT ec
            FROM EventClaim ec
            WHERE ec.eventApplication.session.event.id = :eventId
            AND :sessionId IS NULL OR ec.eventApplication.session.id = :sessionId
            """)
    Page<EventClaim> findByEventIdAndSessionId(UUID eventId, UUID sessionId, Pageable pageable);

    @Query("""
                SELECT COUNT(a) > 0
                FROM EventClaim a
                JOIN a.eventApplication ea
                JOIN ea.session s
                JOIN s.event e
                WHERE a.id = :claimId
                  AND e.host.id = :hostId
            """)
    boolean existsByIdAndHost_Id(UUID claimId, UUID hostId);
}
