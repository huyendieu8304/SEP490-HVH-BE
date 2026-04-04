package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerSimpleResponseForAdmin;
import com.sep490.g28.hvh.be.entity.Volunteer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface VolunteerRepository extends JpaRepository<Volunteer, UUID> {

    boolean existsByCid(String cid);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByNickname(String nickname);

    @Query("""
            SELECT new com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerSimpleResponseForAdmin(
            v.id,
            v.vid,
            v.avatarUrl,
            v.fullName,
            v.cid,
            v.phone,
            v.email,
            v.dob,
            v.activityCount,
            v.avgRating,
            v.creditScore,
            u.status,
            v.address,
            v.detailAddress
            )
            FROM Volunteer v
            LEFT JOIN User u ON u.id = v.id
            WHERE (:email IS NULL OR v.email ILIKE CONCAT('%', CAST(:email AS string), '%'))
            ORDER BY v.createdAt DESC
            """)
    Page<VolunteerSimpleResponseForAdmin> findVolunteersByAdmin(Pageable pageable, String email);

}
