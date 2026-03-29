package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_sessions",
        indexes = {
                @Index(name = "idx_event_sessions_eventId", columnList = "event_id"),
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    //--------------------------------------------------------
    @Column(
            name = "start_date_time",
            nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime startDateTime; // check-in time

    @Column(
            name = "end_date_time",
            nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime endDateTime;   // check-out time

    @Column(name = "expected_vol_amount", nullable = false)
    private int expectedVolAmount;

    @Column(name = "expected_ser_amount", nullable = false)
    private int expectedSerAmount;

    @Column(name = "approved_application_count", nullable = false)
    private int approvedApplicationCount = 0; //increase when an application is approved

    //--------------------------------------------------------
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

    public EventSession(EventSession session) {
        this.id = session.id;
        this.event = session.event;
        this.startDateTime = session.startDateTime;
        this.endDateTime = session.endDateTime;
        this.expectedVolAmount = session.expectedVolAmount;
        this.expectedSerAmount = session.expectedSerAmount;
        this.approvedApplicationCount = session.approvedApplicationCount;
        this.createdAt = session.createdAt;
        this.updatedAt = session.updatedAt;
    }
}
