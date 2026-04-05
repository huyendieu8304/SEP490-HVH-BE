package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.activityDomain.request.ChangeActivityDomainVisibilityRequest;
import com.sep490.g28.hvh.be.dto.activityDomain.request.ChangeActivitySubDomainVisibilityRequest;
import com.sep490.g28.hvh.be.dto.activityDomain.request.CreateActivityDomainRequest;
import com.sep490.g28.hvh.be.dto.activityDomain.request.UpdateActivityDomainRequest;
import com.sep490.g28.hvh.be.dto.activityDomain.response.ActivityDomainDetailsResponse;
import com.sep490.g28.hvh.be.service.ActivityDomainService;
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

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ActivityDomainController {

    ActivityDomainService activityDomainService;

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PostMapping("/sys-admin/activity-domains/create")
    public ResponseEntity<String> createActivityDomain(@RequestBody @Valid CreateActivityDomainRequest request) {
        activityDomainService.createActivityDomain(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/activity-domains/{id}/update")
    public ResponseEntity<String> updateActivityDomain(@PathVariable(name = "id") Short inputId
            , @RequestBody @Valid UpdateActivityDomainRequest request) {
        activityDomainService.updateActivityDomain(inputId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/activity-domains")
    public ResponseEntity<Page<ActivityDomainDetailsResponse>> getActivityDomains(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER") int pageNumber,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE") int pageSize,
            @RequestParam(required = false) String inputActive,
            @RequestParam(required = false) String name
    ) {
        return ResponseEntity.ok(activityDomainService.getActivityDomains(pageNumber, pageSize, inputActive, name));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/activity-domains/{id}/change-visibility")
    public ResponseEntity<String> updateActivityDomain(@PathVariable(name = "id") Short inputId
            , @RequestBody @Valid ChangeActivityDomainVisibilityRequest request) {
        activityDomainService.changeActivityDomainVisibility(inputId, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PutMapping("/sys-admin/activity-domains/activity-subdomains/{id}/change-visibility")
    public ResponseEntity<String> updateActivityDomain(@PathVariable(name = "id") Short inputId
            , @RequestBody @Valid ChangeActivitySubDomainVisibilityRequest request) {
        activityDomainService.changeActivitySubDomainVisibility(inputId, request);
        return ResponseEntity.ok().build();
    }
}
