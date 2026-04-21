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
        name = "volunteer_saved_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_volunteer_saved_events",
                columnNames = {"volunteer_id", "event_id"}
        ),
        indexes = {
                @Index(name = "idx_volunteer_saved_events_volunteer_id", columnList = "volunteer_id"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerSavedEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", referencedColumnName = "id")
    private Volunteer volunteer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event event;

    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime createdAt;
}
