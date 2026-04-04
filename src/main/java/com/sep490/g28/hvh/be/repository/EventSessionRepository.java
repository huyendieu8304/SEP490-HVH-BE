package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface EventSessionRepository extends JpaRepository<EventSession, UUID> {
    @Query(
            """
            SELECT EventSession
            FROM EventSession session
            WHERE session.event.id = :id
            """
    )
    List<EventSession> findByEventId(UUID id);

    @Query(value = """
        SELECT s.*
        FROM event_sessions s
        JOIN events e ON e.id = s.event_id
        WHERE e.host_id = :hostId
          AND e.id <> :eventId
          AND e.status IN (
              'APPROVED_BY_MNG',
              'RECRUITING',
              'UPCOMING',
              'ONGOING'
          )
          AND s.start_date_time::date = ANY(:dates)
        """, nativeQuery = true)
    List<EventSession> findConflictingSessions(
            UUID hostId,
            UUID eventId,
            java.sql.Date[] dates
    );

    @Query(value = """
        SELECT s.*
        FROM event_sessions s
        JOIN events e ON e.id = s.event_id
        WHERE e.host_id = :hostId
          AND e.id <> :eventId
          AND e.status IN (:statuses)
        """, nativeQuery = true)
    List<EventSession> findByHostExcludingEvent(
            UUID hostId,
            UUID eventId,
            List<String> statuses
    );

    boolean existsByIdAndAndEvent_Host_Id(UUID sessionId, UUID hostId);
}
