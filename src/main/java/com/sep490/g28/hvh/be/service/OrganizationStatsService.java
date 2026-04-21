package com.sep490.g28.hvh.be.service;

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
}
