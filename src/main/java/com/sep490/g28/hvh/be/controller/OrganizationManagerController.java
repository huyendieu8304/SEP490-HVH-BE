package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.orgmanager.request.UpdateOrgManagerProfileRequest;
import com.sep490.g28.hvh.be.dto.orgmanager.response.OrgManagerAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.response.UpdateOrgManagerProfileResponse;
import com.sep490.g28.hvh.be.service.OrganizationManagerService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class OrganizationManagerController {

    OrganizationManagerService organizationManagerService;

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @PutMapping("/org-manager/org-managers/update-profile")
    public ResponseEntity<UpdateOrgManagerProfileResponse> updateOrgManagerProfile(
            @RequestBody @Valid UpdateOrgManagerProfileRequest request) {
        return ResponseEntity.ok(organizationManagerService.updateOrgManagerProfile(request));
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/org-managers/account-information")
    public ResponseEntity<OrgManagerAccountInformationResponse> getOrgManagerAccountInformation() {
        return ResponseEntity.ok(organizationManagerService.getOrgManagerAccountInformation());
    }
}
