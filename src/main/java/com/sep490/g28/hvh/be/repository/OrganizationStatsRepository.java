package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.host.payload.TopHostPayload;
import com.sep490.g28.hvh.be.entity.OrganizationStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationStatsRepository extends JpaRepository<OrganizationStats, UUID> {
    @Modifying
    @Query("""
                UPDATE OrganizationStats s
                SET
                    s.approvedApplications = s.approvedApplications + :addApprovedApplications,
                    s.attendedApplications = s.attendedApplications + :addAttendedApplications,
                    s.creditHours = s.creditHours + :addCreditHours,
                    s.completedEvents = s.completedEvents + :addCompletedEvents
                WHERE
                    s.orgId = :orgId
                    AND s.month = :month
                    AND s.year = :year
            """)
    int updateOrganizationStatsBy(
            @Param("orgId") UUID orgId,
            @Param("month") int month,
            @Param("year") int year,
            @Param("addApprovedApplications") int addApprovedApplications,
            @Param("addAttendedApplications") int addAttendedApplications,
            @Param("addCreditHours") int addCreditHours,
            @Param("addCompletedEvents") int addCompletedEvents
    );

    @Modifying
    @Query("""
                UPDATE OrganizationStats s
                SET
                    s.approvedApplications = s.approvedApplications + :addApprovedApplications,
                    s.attendedApplications = s.attendedApplications + :addAttendedApplications,
                    s.creditHours = s.creditHours + :addCreditHours,
                    s.completedEvents = s.completedEvents + :addCompletedEvents,
                    s.topHostPayloads = :topHostPayloads
                WHERE
                    s.orgId = :orgId
                    AND s.month = :month
                    AND s.year = :year
            """)
    int updateOrganizationStatsBy(
            @Param("orgId") UUID orgId,
            @Param("month") int month,
            @Param("year") int year,
            @Param("addApprovedApplications") int addApprovedApplications,
            @Param("addAttendedApplications") int addAttendedApplications,
            @Param("addCreditHours") int addCreditHours,
            @Param("addCompletedEvents") int addCompletedEvents,
            List<TopHostPayload> topHostPayloads
    );

}
