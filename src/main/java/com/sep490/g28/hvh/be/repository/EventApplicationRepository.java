package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.projection.EligibleApplicationProjection;
import com.sep490.g28.hvh.be.dto.eventapplication.response.CompletedApplicationResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.ActualParticipantResponse;
import com.sep490.g28.hvh.be.entity.EventApplication;
import com.sep490.g28.hvh.be.entity.EventSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            WHERE e.session.id = :sessionId
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

    @Query(value = """
            SELECT * FROM event_applications WHERE session_id IN (:sessionIds) AND status IN ('PENDING', 'APPROVED')
            """, nativeQuery = true)
    List<EventApplication> getPendingAndApprovedApplications(List<UUID> sessionIds);

    @Query("""
                SELECT e
                FROM EventApplication e
                WHERE e.volunteer.id = :volunteerId
                AND e.sessionDate = :sessionDate
                AND e.session.checkInCode = :checkInCode
                And e.session.startDateTime <= :checkInTime
                And e.session.endDateTime >= :checkInTime
            """)
    EventApplication findEventApplicationByVolunteerIdAndSessionDate(UUID volunteerId,
                                                                     LocalDate sessionDate,
                                                                     String checkInCode,
                                                                     OffsetDateTime checkInTime);

    @Query("""
                SELECT e
                FROM EventApplication e
                WHERE e.volunteer.id = :volunteerId
                AND e.sessionDate = :sessionDate
                And e.session.startDateTime <= :current
                And e.session.endDateTime >= :current
            """)
    EventApplication findByVolunteerIdAndSessionDate(UUID volunteerId,
                                                     LocalDate sessionDate,
                                                     OffsetDateTime current);

    @Query("""
                SELECT e
                FROM EventApplication e
                WHERE e.volunteer.id = :volunteerId
                AND e.sessionDate = :sessionDate
            """)
    List<EventApplication> findAllByVolunteerIdAndSessionDate(UUID volunteerId, LocalDate sessionDate);

    @Query("""
            SELECT e
            FROM EventApplication e
            WHERE e.volunteer.id = :volunteerId
            AND (:status IS NULL OR e.status = :status)
            """)
    Page<EventApplication> findByVolunteerId(
            UUID volunteerId,
            @Param("status") EEventApplicationStatus status,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM EventApplication e
            WHERE e.volunteer.id = :volunteerId
            AND e.session.id = :sessionId
            AND e.status = 'APPROVED'
            """)
    EventApplication findByVolunteerIdAndSessionId(UUID volunteerId, UUID sessionId);

    @Query("""
            SELECT new com.sep490.g28.hvh.be.dto.volunteer.response.ActualParticipantResponse (
            v.id,
            v.fullName,
            v.bio,
            v.avatarUrl,
            v.address,
            v.nickname,
            v.email,
            v.phone,
            v.creditScore,
            v.honorScore,
            v.avgRating,
            a.id,
            c.checkInTime,
            c.checkOutTime
            ) FROM EventApplication a
            LEFT JOIN CheckInLog c ON a.id = c.eventApplication.id
            LEFT JOIN Volunteer v ON a.volunteer.id = v.id
            WHERE a.session.id = :sessionId
                AND a.status = com.sep490.g28.hvh.be.constant.EEventApplicationStatus.APPROVED
            """)
    Page<ActualParticipantResponse> findCheckedInVolunteer(UUID sessionId, Pageable pageable);

    @Query("""
            SELECT new com.sep490.g28.hvh.be.dto.eventapplication.projection.EligibleApplicationProjection(
            a,
            r,
            v
            ) FROM EventApplication a
            LEFT JOIN a.review r
            LEFT JOIN a.volunteer v
            WHERE a.session.id = :sessionId
            AND a.status = 'COMPLETED'
            AND a.creditHour >= 0
            """)
    List<EligibleApplicationProjection> findEligibleApplicationProjection(UUID sessionId);

    @Query("""
            SELECT a
            FROM EventApplication a
            WHERE a.session.id = :sessionId
             AND a.status = com.sep490.g28.hvh.be.constant.EEventApplicationStatus.APPROVED
            """)
    List<EventApplication> findApprovedApplicationBySessionId(UUID sessionId);

    @Query("""
            SELECT new com.sep490.g28.hvh.be.dto.eventapplication.response.CompletedApplicationResponse (
            v.id,
            v.fullName,
            v.bio,
            v.avatarUrl,
            v.address,
            v.nickname,
            v.email,
            v.phone,
            v.creditScore,
            v.honorScore,
            v.avgRating,
            a.id,
            c.checkInTime,
            c.checkOutTime
            ) FROM EventApplication a
            LEFT JOIN CheckInLog c ON a.id = c.eventApplication.id
            LEFT JOIN Volunteer v ON a.volunteer.id = v.id
            WHERE a.session.id = :sessionId
                AND a.status = com.sep490.g28.hvh.be.constant.EEventApplicationStatus.COMPLETED
            """)
    Page<CompletedApplicationResponse> findCompletedApplications(UUID sessionId, Pageable pageable);

    @Modifying
    @Query(value = """
            UPDATE event_applications
            SET status = 'REJECTED'
            WHERE session_id IN (:sessionIds)
              AND status  = 'PENDING'
            RETURNING *
            """, nativeQuery = true)
    List<EventApplication> rejectApplicationsBySessions(
            List<UUID> sessionIds
    );
}
