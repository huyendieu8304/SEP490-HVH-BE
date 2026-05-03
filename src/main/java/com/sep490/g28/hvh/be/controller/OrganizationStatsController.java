package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationCountHostsAndEventsResponse;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationStatsResponseForManager;
import com.sep490.g28.hvh.be.service.OrganizationStatsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class OrganizationStatsController {

    OrganizationStatsService organizationStatsService;

    //get six months stats
    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/org-stats")
    public ResponseEntity<List<OrganizationStatsResponseForManager>> getOrganizationStats6Months() {
        return ResponseEntity.ok(organizationStatsService.getOrganizations6MonthsStatistics());
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @GetMapping("/org-manager/org-stats/count-hosts-events")
    public ResponseEntity<OrganizationCountHostsAndEventsResponse> countHostsAndEvents() {
        return ResponseEntity.ok(organizationStatsService.countHostsAndEvents());
    }

}
