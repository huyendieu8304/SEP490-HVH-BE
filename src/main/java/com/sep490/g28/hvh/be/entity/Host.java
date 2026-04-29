package com.sep490.g28.hvh.be.entity;

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
@Table(name = "hosts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Host {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(nullable = false, length = 12)
    private String cid; //citizen id

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(name = "full_name", length = 100)
    private String fullName;

    private Boolean gender; //1: male, 0: female

    private LocalDate dob;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 50)
    private String address;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private OrganizationManager createdBy; //de nhu nay thi chi org manager tao duoc tk host

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
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private Organization organization;


}
