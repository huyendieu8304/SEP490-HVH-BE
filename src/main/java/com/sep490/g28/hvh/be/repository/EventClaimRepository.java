package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.EventClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventClaimRepository extends JpaRepository<EventClaim, UUID> {
    EventClaim findByEventApplicationId(UUID eventApplicationId);
}
