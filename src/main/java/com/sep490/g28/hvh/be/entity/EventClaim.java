package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EEventClaimStatus;
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
        name = "event_claims"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventClaim {
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

    @Column(name = "honor_hour", nullable = false)
    private Short honorHour = 0;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "detail_reason", nullable = false, length = 300)
    private String detailReason;

    @Column(name = "evidences", length = 500)
    private String evidences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EEventClaimStatus status;

    //--------------------------------------------------------
    @CreationTimestamp
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime createdAt;
}
