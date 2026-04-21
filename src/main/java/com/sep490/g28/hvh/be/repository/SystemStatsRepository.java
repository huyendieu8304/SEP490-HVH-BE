package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.SystemStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemStatsRepository extends JpaRepository<SystemStats, UUID> {

    Optional<SystemStats> findByYearAndMonth(int year, int month);
}
