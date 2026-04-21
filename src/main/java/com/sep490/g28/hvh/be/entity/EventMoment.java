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
        name = "event_moments"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventMoment {
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

    @Column(name = "moment_pictures", length = 500)
    private String momentPictures;

    @Column(name = "moment_content", length = 500)
    private String momentContent;

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
}
