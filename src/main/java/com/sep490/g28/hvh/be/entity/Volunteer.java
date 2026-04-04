package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EEducationLevel;
import com.sep490.g28.hvh.be.constant.EEmployStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "volunteers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Volunteer {
    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(unique = true, updatable = false, nullable = false)
    private UUID vid; //volunteer id

    @Column(unique = true, nullable = false, length = 12)
    private String cid; //citizen id

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false, length = 10)
    private String phone;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(unique = true, length = 50)
    private String nickname;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(length = 100)
    private String bio;

    private boolean gender; //1: male, 0: female

    private LocalDate dob;

    private Short level = 0;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 50)
    private String address;

    @Column(name = "detail_address", length = 100)
    private String detailAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "employ_status", length = 30)
    private EEmployStatus employStatus;

    @Column(name = "work_address")
    private String workAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "education_level", length = 30)
    private EEducationLevel educationLevel;

    @Column(length = 50)
    private String sid; //student id

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
    private SystemAdmin createdBy;

    @Column(name = "credit_score", nullable = false)
    private int creditScore = 0;

    @Column(name = "honor_score", nullable = false)
    private int honorScore = 0;

    @Column(name = "device_id", nullable = true)
    private String deviceId;

    //--------------------------------------------------------
    @Column(name = "avg_rating", nullable = false)
    private Short avgRating = 0;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount = 0;

    @PrePersist
    void prePersist() {
        if (creditScore == null) creditScore = 0;
        if (honorScore == null) honorScore = 0;
    }
}
