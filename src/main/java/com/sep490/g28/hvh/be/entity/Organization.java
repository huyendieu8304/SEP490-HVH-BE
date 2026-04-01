package com.sep490.g28.hvh.be.entity;

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
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {
    @Id
    @GeneratedValue
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

    @Column(name = "legal_document", nullable = false)
    private String legalDocument;

    @Column(name = "other_evidences", length = 500)
    private String otherEvidences;

    @Column(name = "avatar_image", length = 150)
    private String avatarImage;

    @Column(name = "cover_image", length = 150)
    private String coverImage;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private SystemAdmin createBy;

    @Column(name = "credit_hour", nullable = false)
    private int creditHour = 0;

    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private OrganizationManager organizationManager;
}