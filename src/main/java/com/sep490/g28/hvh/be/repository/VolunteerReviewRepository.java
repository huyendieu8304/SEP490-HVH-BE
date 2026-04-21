package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.VolunteerReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VolunteerReviewRepository extends JpaRepository<VolunteerReview, UUID> {
    Optional<VolunteerReview> findByEventApplication_Id(UUID eventApplicationId);
}
