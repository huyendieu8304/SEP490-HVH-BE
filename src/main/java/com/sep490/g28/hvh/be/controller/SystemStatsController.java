package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.systemstats.response.SystemStatsResponse;
import com.sep490.g28.hvh.be.service.SystemStatsService;
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
public class SystemStatsController {

    SystemStatsService systemStatsService;

    //get six months stats
    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/sys-stats")
    public ResponseEntity<List<SystemStatsResponse>> getSystemStats6Months() {
        return ResponseEntity.ok(systemStatsService.getSystem6MonthsStatistics());
    }

}
