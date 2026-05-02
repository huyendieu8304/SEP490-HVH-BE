package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.volunteerreview.response.VolunteerReviewResponse;
import com.sep490.g28.hvh.be.entity.VolunteerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface VolunteerReviewRepository extends JpaRepository<VolunteerReview, UUID> {
    Optional<VolunteerReview> findByEventApplication_Id(UUID eventApplicationId);


    @Query("""
                    SELECT new com.sep490.g28.hvh.be.dto.volunteerreview.response.VolunteerReviewResponse(
                    r.id,
                    e.name,
                    s.startDateTime,
                    s.endDateTime,
                    r.professionalAttitudeRating,
                    r.responsibilityPunctualityRating,
                    r.workEffectivenessRating,
                    r.teamworkCommunicationRating,
                    r.adaptabilityProblemSolvingRating,
                    r.avgRating,
                    r.comment
                    ) from VolunteerReview r
                    left join EventApplication a on r.eventApplication.id = a.id
                    left join EventSession s on a.session.id = s.id
                    left join Event e on s.event.id = e.id
                    WHERE a.volunteer.id = :volunteerId
            """)
    Page<VolunteerReviewResponse> getReviewsOfVolunteer(UUID volunteerId, Pageable pageable);
}
