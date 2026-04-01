package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CheckInLogRepository extends JpaRepository<CheckInLog, UUID> {

    @Query("""
            SELECT c
            FROM CheckInLog c
            WHERE c.session.id = :eventSessionId
            AND c.volunteer.id = :volunteerId
            """)
    CheckInLog findByEventSessionIdAndVolunteerId(UUID eventSessionId, UUID volunteerId);

    @Query("""
            SELECT COUNT(c) > 0
            FROM CheckInLog c
            WHERE c.deviceId = :deviceId
            AND c.apVersion = :apVersion
            AND c.osVersion = :osVersion
            """)
    boolean existsByDevice(String deviceId, String apVersion, String osVersion);

    @Query("""
            SELECT COUNT(c) > 0
            FROM CheckInLog c
            WHERE c.volunteer.id = :volunteerId
            AND c.deviceId = :deviceId
            AND c.apVersion = :apVersion
            AND c.osVersion = :osVersion
            """)
    boolean existsByDeviceAndVolunteerId(String deviceId, String apVersion, String osVersion, UUID volunteerId);
}
