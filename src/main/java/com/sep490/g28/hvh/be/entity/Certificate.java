package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.ECertificateStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "certificates"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", unique = true, nullable = false)
    private String code; //the public code of the cert (verify)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", referencedColumnName = "id", nullable = false)
    private Volunteer volunteer;

    @Column(name = "certificate_path", nullable = false)
    private String certificatePath;

    @CreationTimestamp
    @Column(
            name = "issued_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE"
    )
    private OffsetDateTime issuedAt; //create at

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    //todo, set default in migration code
    private ECertificateStatus status = ECertificateStatus.ACTIVE; //ACTIVE/REVOKED

}
