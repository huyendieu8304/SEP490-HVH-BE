package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.request.UpdateHostProfileRequest;
import com.sep490.g28.hvh.be.dto.host.response.*;
import com.sep490.g28.hvh.be.service.HostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class HostController {

    HostService hostService;

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @PostMapping("/org-manager/hosts")
    public ResponseEntity<Void> createAccount(
            @RequestBody @Valid CreateHostAccountRequest request
    ) {
        hostService.createHostAccount(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/hosts")
    public ResponseEntity<Page<HostSimpleResponseForManager>> getHostsByManager(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String email
    ) {
        return ResponseEntity.ok(hostService.getHostsByManager(pageNumber, pageSize, email));
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @hostAuthorizer.isManagerOfHost(#id)")
    @GetMapping("/org-manager/hosts/{id}/info")
    public ResponseEntity<HostInfoResponseForManager> getHostInfoByManager(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(hostService.getHostInfoByManager(id));
    }

    @PreAuthorize("hasRole('ORG_MANAGER') and @hostAuthorizer.isManagerOfHost(#hostId)")
    @GetMapping("/org-manager/hosts/{hostId}/activities")
    public ResponseEntity<Page<HostActivitiesResponseForManager>> getHostActivitiesByManager(
            @PathVariable UUID hostId,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam
            LocalDate fromDate,

            @RequestParam
            LocalDate toDate
    ) {
        return ResponseEntity.ok(hostService.getHostActivitiesByManager(hostId, pageNumber, pageSize, fromDate, toDate));
    }

    @PreAuthorize("hasRole('HOST')")
    @PutMapping("/host/hosts/update-profile")
    public ResponseEntity<UpdateHostProfileResponse> updateHostProfile(
            @RequestBody @Valid UpdateHostProfileRequest request
    ) {
        return ResponseEntity.ok(hostService.updateHostProfile(request));
    }

    @PreAuthorize("hasRole('HOST')")
    @GetMapping("/host/hosts/account-information")
    public ResponseEntity<HostAccountInformationResponse> getHostAccountInformation() {
        return ResponseEntity.ok(hostService.getHostAccountInformation());
    }
}
