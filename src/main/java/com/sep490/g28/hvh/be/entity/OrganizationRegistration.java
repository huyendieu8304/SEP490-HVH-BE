package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EOrgRegistrationStatus;
import com.sep490.g28.hvh.be.constant.EOrgType;
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
@Table(name = "org_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRegistration {
    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "dha_registered", nullable = false)
    private Boolean dhaRegistered;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", length = 50, nullable = false)
    private EOrgType orgType;

    @Column(name = "org_introduction", length = 500, nullable = false)
    private String orgIntroduction;

    @Column(name = "manager_full_name", nullable = false, length = 100)
    private String managerFullName;

    @Column(name = "manager_cid", nullable = false, length = 12)
    private String managerCid;

    @Column(name = "manager_phone", nullable = false, length = 10)
    private String managerPhone;

    @Column(name = "manager_email", nullable = false)
    private String managerEmail;

    @Column(name = "manager_cid_front", nullable = false)
    private String managerCidFront;

    @Column(name = "manager_cid_back", nullable = false)
    private String managerCidBack;

    @Column(name = "manager_cid_holding", nullable = false)
    private String managerCidHolding;

    @Column(name = "legal_document", nullable = false)
    private String legalDocument;

    @Column(name = "other_evidences", length = 500)
    private String otherEvidences;

    @Column(name = "application_reason")
    private String applicationReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private EOrgRegistrationStatus status;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_manager_id", referencedColumnName = "id")
    private OrganizationManager orgManager;
}