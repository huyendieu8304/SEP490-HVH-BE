package com.sep490.g28.hvh.be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "activity_domains")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDomain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "smallint") //có the co loi voi cai nay nhung hien tai chua sua duoc (insert tay roi insert bang code)
    private Short id;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "special_session_max_time")
    private Short specialSessionMaxTime;

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

    @JsonIgnore
    @OneToMany(mappedBy = "activityDomain")
    private List<ActivitySubDomain> activitySubDomains;
}
