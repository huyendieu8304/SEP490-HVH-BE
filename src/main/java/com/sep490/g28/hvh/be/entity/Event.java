package com.sep490.g28.hvh.be.entity;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.constant.EServedTarget;
import com.sep490.g28.hvh.be.constant.EServingPlaceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
//todo, có khi thêm index tren satus nua, search cho nhanh
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", referencedColumnName = "id", nullable = false)
    private Host host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private Organization organization;

    //--------------------------------------------------------
    @Column(nullable = false)
    private String name;

    @OneToMany(
            mappedBy = "event",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<EventImage> images = new ArrayList<>();


    @Column(name = "description", columnDefinition = "text", nullable = false)
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    //--------------------------------------------------------
    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove; //1: yes, 0: no

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_sub_domain_id", referencedColumnName = "id", nullable = false)
    private ActivitySubDomain activitySubDomain;

    @Column(name = "served_target")
    @Enumerated(EnumType.STRING)
    private EServedTarget servedTarget;

    @Column(name = "serving_place_type")
    @Enumerated(EnumType.STRING)
    private EServingPlaceType servingPlaceType;

    //--------------------------------------------------------
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;

    @Column(name = "recruitment_end_date", nullable = false)
    private LocalDate recruitmentEndDate;

    @OneToMany(
            mappedBy = "event",
            cascade = CascadeType.ALL,
            orphanRemoval = true //each checkin place must link to one event
    )
    private List<EventSession> dateTimes = new ArrayList<>();


    //--------------------------------------------------------
    /**
     * geography(Point, 4326)
     * Save using PostGIS
     */
    @Column(
            name = "check_in_location",
            nullable = false,
            columnDefinition = "geography(Point, 4326)"
    )
    private Point checkInLocation;

    @Column(name = "check_in_accuracy_meters", nullable = false)
    private Double checkInAccuracyMeters;

    @Column(name = "check_in_code", length = 6)
    private String checkInCode;

    //--------------------------------------------------------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EEventStatus status;

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
    private Host createBy;

}
