package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.systemadmin.request.UpdateSystemAdminProfileRequest;
import com.sep490.g28.hvh.be.dto.systemadmin.response.SystemAdminAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.systemadmin.response.UpdateSystemAdminProfileResponse;
import com.sep490.g28.hvh.be.service.SystemAdminService;
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
public class SystemAdminController {

    SystemAdminService systemAdminService;

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/sys-admins/account-information")
    public ResponseEntity<SystemAdminAccountInformationResponse> getSystemAdminAccountInformation() {
        return ResponseEntity.ok(systemAdminService.getSystemAdminAccountInformation());
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/sys-admins/update-profile")
    public ResponseEntity<UpdateSystemAdminProfileResponse> updateSystemAdminProfile(
            @RequestBody @Valid UpdateSystemAdminProfileRequest request) {
        return ResponseEntity.ok(systemAdminService.updateSystemAdminProfile(request));
    }
}
