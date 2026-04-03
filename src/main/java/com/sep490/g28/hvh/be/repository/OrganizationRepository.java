package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.Organization;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    @Query(value = """
            SELECT o.id, o.name, o.org_type,
                   COUNT(DISTINCT e.id) AS totalEvents
            FROM organizations o
            LEFT JOIN events e ON e.organization_id = o.id
            WHERE (:name IS NULL OR o.name ILIKE CONCAT('%', :name, '%'))
            AND (:orgTypes IS NULL OR o.org_type IN (:orgTypes))
            GROUP BY o.id
            -- #pageable
            """, nativeQuery = true)
    List<Object[]> search(
            @Param("name") String name,
            @Param("orgTypes") List<String> orgTypes,
            Pageable pageable);
}
