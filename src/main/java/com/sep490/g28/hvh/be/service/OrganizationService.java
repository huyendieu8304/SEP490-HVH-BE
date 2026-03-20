package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.organization.request.OrganizationRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.organization.request.RegisterOrganizationRequest;
import com.sep490.g28.hvh.be.dto.organization.response.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface OrganizationService {
    RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest registerOrganizationRequest);

    Page<OrganizationRegistrationSimpleResponse> getOrgRegistrations(
            int pageNumber, int pageSize, String inputStatus, @Email String managerEmail);

    OrganizationRegistrationDetailsResponse getOrgRegistrationDetails(UUID id);

    void verifyOrgRegistration(UUID id, @Valid OrganizationRegistrationVerifyRequest request);

    Page<OrganizationSimpleResponse> getOrganizations(
            int pageNumber, int pageSize, String name, List<String> orgTypeLists);

    OrganizationDetailsResponseForSystemAdmin getOrganizationDetailsBySystemAdmin(UUID ordId);

    OrganizationDetailsResponse getOrganizationDetails(UUID ordId);
}
