package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.dto.activityDomain.payload.CountEventInDomainPayload;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "sys_stats",
        uniqueConstraints = {
                @UniqueConstraint(name = "sys_stats_year_month", columnNames = {"year", "month"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemStats {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(name = "verified_volunteers")
    private int verifiedVolunteers = 0;

    @Column(name = "verified_organizations")
    private int verifiedOrganizations = 0;

    @Column(name = "completed_events")
    private int completedEvents = 0;

    @Column(name = "credit_hours")
    private int creditHours = 0;

    @Column(name = "approved_applications")
    private int approvedApplications = 0;

    @Column(name = "attended_applications")
    private int attendedApplications = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "count_event_in_domain", columnDefinition = "jsonb")
    private List<CountEventInDomainPayload> countEventInDomainPayloads = new ArrayList<>();

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(
            name = "updated_at",
            nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime updatedAt;
}
