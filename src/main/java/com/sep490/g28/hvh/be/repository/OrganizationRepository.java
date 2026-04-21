package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    @Query(value = """
            SELECT o.id, o.name, o.org_type,
                   COUNT(DISTINCT e.id) AS totalEvents, o.credit_hour
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

    @Query(value = """
            SELECT o.id, o.name, o.org_type,
                   COUNT(DISTINCT e.id) AS totalEvents, o.credit_hour
            FROM organizations o
            LEFT JOIN events e ON e.organization_id = o.id
            WHERE (:name IS NULL OR o.name ILIKE CONCAT('%', :name, '%'))
            GROUP BY o.id
            -- #pageable
            """, nativeQuery = true)
    List<Object[]> searchWithoutOrgType(
            @Param("name") String name,
            Pageable pageable);

    @Query("""
            SELECT o
            FROM Organization o
            WHERE (:name IS NULL OR o.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            AND (o.orgType IN (:orgTypes))
            """)
    Page<Organization> searchByAdmin(
            @Param("name") String name,
            @Param("orgTypes") List<String> orgTypes,
            Pageable pageable);

    @Query("""
            SELECT o
            FROM Organization o
            WHERE (:name IS NULL OR o.name ILIKE CONCAT('%', CAST(:name AS string), '%'))
            """)
    Page<Organization> searchByAdminWithoutOrgType(
            @Param("name") String name,
            Pageable pageable);

    Organization findByOrganizationManager_Id(UUID organizationManagerId);
}
