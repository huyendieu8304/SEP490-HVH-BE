package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "check_in_logs",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"session_id", "volunteer_id"})
        },
        indexes = {
                @Index(name = "idx_check_in_logs_volunterid_sessionid", columnList = "volunteer_id, session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckInLog {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", referencedColumnName = "id")
    private Volunteer volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private EventSession session;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "ap_version", nullable = false)
    private String apVersion;

    @Column(name = "os_version", nullable = false)
    private String osVersion;

    //--------------------------------------------------------
    /**
     * geography(Point, 4326)
     * Save using PostGIS
     */
    @Column(
            name = "check_in_location",
            nullable = false,
            columnDefinition = "geography(Point, 4326)"
    )
    private Point checkInLocation;

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
