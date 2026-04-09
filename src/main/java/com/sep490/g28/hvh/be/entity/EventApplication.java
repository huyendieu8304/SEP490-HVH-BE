package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_applications",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"session_id", "volunteer_id"})
        },
        indexes = {
                @Index(name = "idx_event_applications_volunterid_sessionid", columnList = "volunteer_id, session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventApplication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", referencedColumnName = "id")
    private Volunteer volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id")
    private EventSession session;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EEventApplicationStatus status;

    @Column(name = "credit_hour")
    private Short creditHour;

    @Column(name = "honor_hour")
    private Short honorHour;

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

    @OneToOne(mappedBy = "eventApplication", fetch = FetchType.LAZY)
    private VolunteerReview review;
}
