package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.eventsession.projection.SessionEventProjection;
import com.sep490.g28.hvh.be.entity.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
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

    @Query("""
            SELECT new com.sep490.g28.hvh.be.dto.eventsession.projection.SessionEventProjection(
            s,
            e.id,
            e.name,
            e.host.id
            )
            FROM EventSession s
            LEFT JOIN Event e ON e.id = s.event.id
            WHERE s.startDateTime BETWEEN :start AND :end
            AND e.status = com.sep490.g28.hvh.be.constant.EEventStatus.ONGOING
            """)
    List<SessionEventProjection> findSessionHostedOfOngoingEventBetweenIncluded(
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Modifying
    @Query("""
                UPDATE EventSession es
                SET es.checkInCode = NULL
                WHERE es.endDateTime <= :endOfYesterday
                    AND es.checkInCode IS NOT NULL
            """)
    void clearOldCheckInCode(OffsetDateTime endOfYesterday);
}
