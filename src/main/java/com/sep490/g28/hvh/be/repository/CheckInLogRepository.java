package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CheckInLogRepository extends JpaRepository<CheckInLog, UUID> {

    @Query("""
            SELECT c
            FROM CheckInLog c
            WHERE c.eventApplication.id= :eventApplicationId
            """)
    CheckInLog findByEventApplicationId(UUID eventApplicationId);

    @Query("""
            SELECT COUNT(c) > 0
            FROM CheckInLog c
            WHERE c.eventApplication.id = :eventApplicationId
            AND c.deviceId = :deviceId
            AND c.apVersion = :apVersion
            AND c.osVersion = :osVersion
            """)
    boolean existsByDeviceAndEventApplication(String deviceId, String apVersion, String osVersion, UUID eventApplicationId);
}
