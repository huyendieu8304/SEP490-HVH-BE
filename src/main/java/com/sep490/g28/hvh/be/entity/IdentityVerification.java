package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EVolunteerVerificationStatus;
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
@Table(name = "identity_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IdentityVerification {
    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, length = 12)
    private String cid; //citizen id

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "cid_front", nullable = false)
    private String cidFront;

    @Column(name = "cid_back", nullable = false)
    private String cidBack;

    @Column(name = "cid_holding", nullable = false)
    private String cidHolding;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EVolunteerVerificationStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

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
            name = "reviewed_at",
            nullable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", referencedColumnName = "id")
    private SystemAdmin reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "volunteer_id",
            referencedColumnName = "id"
    )
    private Volunteer volunteer;
}