package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_ratings"
        )
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "application_id",
            referencedColumnName = "id",
            unique = true,
            nullable = false
    )
    private EventApplication eventApplication;

    //--------------------------------------------------------
    @Column(name = "organization_quality_rating", nullable = false)
    private Short organizationQualityRating;

    @Column(name = "professionalism_rating", nullable = false)
    private Short professionalismRating;

    @Column(name = "work_environment_rating", nullable = false)
    private Short workEnvironmentRating;

    @Column(name = "value_impact_rating", nullable = false)
    private Short valueImpactRating;

    @Column(name = "support_connection_rating", nullable = false)
    private Short supportConnectionRating;

    @Column(name = "avg_rating", nullable = false)
    private Short avgRating;

    //--------------------------------------------------------
    @PrePersist
    @PreUpdate
    private void calculateAvgRating() {
        double avg = (
                organizationQualityRating +
                        professionalismRating +
                        workEnvironmentRating +
                        valueImpactRating +
                        supportConnectionRating
        ) / 5.0;

        this.avgRating = (short) Math.round(avg);
    }

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime createdAt;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "event_id", referencedColumnName = "id")
//    private Event event;


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "event_session_id", referencedColumnName = "id")
//    private EventSession eventSession;
}
