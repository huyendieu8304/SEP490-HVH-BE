package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.EventMoment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventMomentRepository extends JpaRepository<EventMoment, UUID> {
    EventMoment findByEventApplicationId(UUID eventApplicationId);

    @Query("""
            SELECT em
            FROM EventMoment em
            WHERE (:name IS NULL OR em.eventApplication.session.event.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            """)
    Page<EventMoment> findAllByName(@Param("name") String eventName, Pageable pageable);

    @Query("""
            SELECT em
            FROM EventMoment em
            WHERE em.eventApplication.volunteer.id = :volunteerId
            AND (:name IS NULL OR em.eventApplication.session.event.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            """)
    Page<EventMoment> findAllByVolunteerId(UUID volunteerId, @Param("name") String eventName, Pageable pageable);
}
