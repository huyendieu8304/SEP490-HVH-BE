package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventApplicationRepository extends JpaRepository<EventApplication, UUID> {
    @Query(
            value = """
                SELECT e.*
                FROM event_applications e
                WHERE volunteer_id = :volunteerId
                  AND session_id = :sessionId
                  AND status IN ('PENDING', 'APPROVED')
                """,
            nativeQuery = true)
    Optional<EventApplication> findApplicationPendingOrApproved(UUID volunteerId, UUID sessionId);


    @Query(
            value = """
                    SELECT s.*
                    FROM event_applications a
                    JOIN event_sessions s
                    ON a.session_id = s.id
                    WHERE a.volunteer_id = :volunteerId
                    AND a.session_date = :applyingDate
                    AND a.status IN ('PENDING','APPROVED')
                    AND s.start_date_time < :applyingEndDateTime
                    AND s.end_date_time > :applyingStartDateTime
                    """,
            nativeQuery = true
    )
    EventSession findOverlapSession(UUID volunteerId, LocalDate applyingDate, OffsetDateTime applyingStartDateTime, OffsetDateTime applyingEndDateTime);


    @Query("""
                SELECT COUNT(a) > 0
                FROM EventApplication a
                JOIN a.session s
                JOIN s.event e
                WHERE a.id = :applicationId
                  AND e.host.id = :hostId
            """)
    boolean existsByIdAndHostId(UUID applicationId, UUID hostId);

    @Query("""
            SELECT e
            FROM EventApplication e
            WHERE e.id = :sessionId
            """)
    Page<EventApplication> getEventApplicationsBySessionId(UUID sessionId, Pageable pageable);

    boolean existsByIdAndVolunteer_Id(UUID applicationId, UUID volunteerId);

    @Modifying
    @Query(value = """
            UPDATE event_applications
            SET status = 'CANCELLED'
            WHERE session_id IN (:sessionIds)
              AND status IN ('PENDING', 'APPROVED')
            RETURNING *
            """, nativeQuery = true)
    List<EventApplication> cancelApplicationsBySessions(
            List<UUID> sessionIds
    );
}
