package com.sep490.g28.hvh.be.repository;

import com.sep490.g28.hvh.be.entity.OrganizationManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationManagerRepository extends JpaRepository<OrganizationManager, UUID> {

    OrganizationManager findByOrganizationId(UUID orgId);
}
