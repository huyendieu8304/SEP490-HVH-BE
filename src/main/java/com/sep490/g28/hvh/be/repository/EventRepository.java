package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.event.projection.EventOrganizationProjection;
import com.sep490.g28.hvh.be.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:name IS NULL OR e.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            AND (:address IS NULL OR e.address ILIKE CONCAT('%', CAST(:address AS string), '%'))
            AND (CAST(:startDate AS DATE) IS NULL OR e.startDate >= :startDate)
            AND (CAST(:endDate AS DATE) IS NULL OR e.startDate <= :endDate)
            AND e.status = 'RECRUITING'
            """)
    Page<Event> search(
            @Param("name") String name,
            @Param("address") String address,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:name IS NULL OR e.name ILIKE CONCAT('%', :name, '%'))
            AND (:address IS NULL OR e.address ILIKE CONCAT('%', :address, '%'))
            AND (CAST(:startDate AS DATE) IS NULL OR e.startDate >= :startDate)
            AND (CAST(:endDate AS DATE) IS NULL OR e.startDate <= :endDate)
            AND (e.activitySubDomain.id IN (:activitySubDomainIds))
            AND e.status = 'RECRUITING'
            """)
    Page<Event> searchWithActivitySubDomain(
            @Param("name") String name,
            @Param("address") String address,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("activitySubDomainIds") List<Short> activitySubDomainIds,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:name IS NULL OR e.name ILIKE CONCAT('%', :name, '%'))
            AND (:address IS NULL OR e.address ILIKE CONCAT('%', :address, '%'))
            AND (:startDate IS NULL OR e.startDate >= :startDate)
            AND (:endDate IS NULL OR e.startDate <= :endDate)
            AND e.status = 'RECRUITING'
            AND e.createdAt > :since
            """)
    Page<Event> refresh(
            @Param("name") String name,
            @Param("address") String address,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            OffsetDateTime since,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:name IS NULL OR e.name ILIKE CONCAT('%', :name, '%'))
            AND (:address IS NULL OR e.address ILIKE CONCAT('%', :address, '%'))
            AND (:startDate IS NULL OR e.startDate >= :startDate)
            AND (:endDate IS NULL OR e.startDate <= :endDate)
            AND (e.activitySubDomain.id IN (:activitySubDomainIds))
            AND e.status = 'RECRUITING'
            AND e.createdAt > :since
            """)
    Page<Event> refreshWithActivitySubDomain(
            @Param("name") String name,
            @Param("address") String address,
            @Param("startDate")LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("activitySubDomainIds") List<Short> activitySubDomainIds,
            OffsetDateTime since,
            Pageable pageable);

    @Query(value = """
            SELECT e.*
            FROM events e
            WHERE e.organization_id = :organizationId
            AND (e.status IN (:status))
            AND (:name IS NULL OR e.name ILIKE CONCAT('%', :name, '%'))
            -- #pageable
            """,
            nativeQuery = true)
    Page<Event> findEventsByOrganizationIdAnd(
            @Param("organizationId") UUID organizationId,
            @Param("status") List<String> status,
            @Param("name") String name,
            Pageable pageable
    );

    @Query(value = """
            SELECT e.*
            FROM events e
            WHERE (e.status IN (:status))
            AND (:name IS NULL OR e.name ILIKE CONCAT('%', :name, '%'))
            -- #pageable
            """,
            nativeQuery = true)
    Page<Event>  findEventsByAdminAnd(
            @Param("status") List<String> status,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("""
            SELECT e
            FROM Event e
            WHERE :orgId IS NULL OR e.organization.id = :orgId
            """)
    List<Event> findAllByOrganizationId(UUID orgId);

    boolean existsByIdAndHost_Id(UUID eventId, UUID hostId);

    boolean existsByIdAndOrganization_Id(UUID eventId, UUID orgId);

    @Query("""
            SELECT e
            FROM Event e
            WHERE e.host.id = :hostId
            AND (e.status = :status)
            AND (:name IS NULL OR e.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            """)
    Page<Event> findEventsByHostId(
            @Param("hostId") UUID hostId,
            @Param("status") EEventStatus status,
            @Param("name") String name,
            Pageable pageable);

    @Query("""
                SELECT e
                FROM Event e
                WHERE e.status = com.sep490.g28.hvh.be.constant.EEventStatus.RECRUITING
                  AND e.recruitmentEndDate <= :targetDate
            """)
    List<Event> findRecruitingEventsAndRecruitmentEndDateBefore(LocalDate targetDate);

    @Query("""
                SELECT e
                FROM Event e
                WHERE e.status = com.sep490.g28.hvh.be.constant.EEventStatus.UPCOMING
                  AND e.startDate <= :targetDate
            """)
    List<Event> findUpcomingEventsAndStartDateToday(LocalDate targetDate);

    @Query("""
                SELECT e
                FROM Event e
                WHERE e.status = com.sep490.g28.hvh.be.constant.EEventStatus.ONGOING
                  AND e.endDate <= :targetDate
            """)
    List<Event> findOngoingEventsAndEndDateYesterday(LocalDate targetDate);

    @Query("""
                SELECT e
                FROM Event e
                WHERE e.status = com.sep490.g28.hvh.be.constant.EEventStatus.ENDED
                  AND e.endDate <= :targetDate
            """)
    List<Event> findEndedEventsAndEndDateBefore(@Param("targetDate") LocalDate targetDate);

    @Query("""
                SELECT new com.sep490.g28.hvh.be.dto.event.projection.EventOrganizationProjection(
                    e,
                    o
                )
                FROM Event e
                LEFT JOIN Organization o ON e.organization.id = o.id
                WHERE e.status = com.sep490.g28.hvh.be.constant.EEventStatus.COMPLETED
                  AND e.endDate <= :targetDate
            """)
    List<EventOrganizationProjection> findCompletedEventsAndEndDateBefore(LocalDate targetDate);
}
