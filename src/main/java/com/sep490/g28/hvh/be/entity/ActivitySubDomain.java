package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "activity_sub_domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySubDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "smallint")
    private Short id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "activity_domain_id",
            nullable = false,
            columnDefinition = "smallint"
    )
    private ActivityDomain activityDomain;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

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
}
