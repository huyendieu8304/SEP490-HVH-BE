package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationCountHostsAndEventsResponse;
import com.sep490.g28.hvh.be.dto.organizationstats.response.OrganizationStatsResponseForManager;

import java.util.List;
import java.util.UUID;

public interface OrganizationStatsService {
    void updateOrganizationCreditHoursAndCountApplicationsAndCountCompletedEventStats(
            UUID orgId,
            int month,
            int year,
            int addApprovedApplications,
            int addAttendedApplications,
            int addCreditHours
    );

    void compileOrganizationsMonthlyStatistics();

    //todo remove this after finish mock data
    void compileOrganizationsMonthlyStatistics(int year, int month);

    List<OrganizationStatsResponseForManager> getOrganizations6MonthsStatistics();

    OrganizationCountHostsAndEventsResponse countHostsAndEvents();

}
