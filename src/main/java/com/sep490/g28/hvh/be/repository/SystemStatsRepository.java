package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.SystemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemStatsRepository extends JpaRepository<SystemStats, UUID> {

    Optional<SystemStats> findByYearAndMonth(int year, int month);

    @Query("""
        SELECT s FROM SystemStats s
        WHERE (s.year * 100 + s.month) >= :fromYm
        ORDER BY (s.year * 100 + s.month) DESC
    """)
    List<SystemStats> findLast6MonthsStats(int fromYm);
}
