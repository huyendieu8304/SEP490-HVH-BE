package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
