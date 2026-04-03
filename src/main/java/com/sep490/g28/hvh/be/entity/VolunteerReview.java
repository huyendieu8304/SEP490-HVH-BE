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
        name = "volunteer_reviews"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerReview {

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
    @Column(name = "professional_attitude_rating", nullable = false)
    private Short professionalAttitudeRating;

    @Column(name = "responsibility_punctuality_rating", nullable = false)
    private Short responsibilityPunctualityRating;

    @Column(name = "work_effectiveness_rating", nullable = false)
    private Short workEffectivenessRating;

    @Column(name = "teamwork_communication_rating", nullable = false)
    private Short teamworkCommunicationRating;

    @Column(name = "adaptability_problem_solving_rating", nullable = false)
    private Short adaptabilityProblemSolvingRating;

    @Column(name = "avg_rating", nullable = false)
    private Short avgRating;

    @Column(name = "comment", length = 250)
    private String comment;

    //--------------------------------------------------------
    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime createdAt;

    //--------------------------------------------------------
    @PrePersist
    @PreUpdate
    private void validateAndCalculate() {
        double avg = (
                professionalAttitudeRating +
                        responsibilityPunctualityRating +
                        workEffectivenessRating +
                        teamworkCommunicationRating +
                        adaptabilityProblemSolvingRating
        ) / 5.0;

        this.avgRating = (short) Math.round(avg);
    }
}
