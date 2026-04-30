package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.VolunteerSavedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface VolunteerSavedEventRepository extends JpaRepository<VolunteerSavedEvent, UUID> {

    @Query("""
            SELECT vse.event
            FROM VolunteerSavedEvent vse
            WHERE vse.volunteer.id = :volunteerId
            AND (:name IS NULL OR vse.event.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            """)
    Page<Event> findAllSavedEventsByVolunteerId(UUID volunteerId, @Param("name") String name, Pageable pageable);

    @Query(""" 
            SELECT vse
            FROM VolunteerSavedEvent vse
            WHERE vse.volunteer.id = :volunteerId
            AND vse.event.id = :eventId
            """)
    VolunteerSavedEvent findByVolunteerIdAndEventId(UUID volunteerId, UUID eventId);
}
