package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.entity.OrganizationStats;
import com.sep490.g28.hvh.be.entity.SystemStats;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.SystemStatsService;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

public class SystemStatsServiceImpl implements SystemStatsService {

    OrganizationStatsRepository organizationStatsRepository;
    SystemStatsRepository systemStatsRepository;
    VolunteerRepository volunteerRepository;
    OrganizationRepository organizationRepository;
    EventRepository eventRepository;


    public void compileSystemStatsDaily() {

        YearMonth yearMonth = YearMonth.now();
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        List<OrganizationStats> organizationStats = organizationStatsRepository.findStatsBy(year, month);

        SystemStats systemStats = systemStatsRepository.findByYearAndMonth(year, month).orElseGet(() -> {
            SystemStats systemStat = new SystemStats();
            systemStat.setYear(year);
            systemStat.setMonth(month);
            return systemStat;
        });

        int completedEvents = 0;
        int creditHours = 0;
        int approvedApplications = 0;
        int attendedApplications = 0;
        for (OrganizationStats organizationStat : organizationStats) {
            completedEvents += organizationStat.getCompletedEvents();
            creditHours += organizationStat.getCreditHours();
            approvedApplications += organizationStat.getApprovedApplications();
            attendedApplications += organizationStat.getAttendedApplications();
        }
        systemStats.setCompletedEvents(completedEvents);
        systemStats.setCreditHours(creditHours);
        systemStats.setApprovedApplications(approvedApplications);
        systemStats.setAttendedApplications(attendedApplications);

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

        LocalDate yesterday = LocalDate.now(zone).minusDays(1);

        OffsetDateTime start = yesterday.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = yesterday.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        //count volunteer verified yesterday
        int countNewVolunteer = volunteerRepository.countCreatedBetween(start, end);
        systemStats.setVerifiedVolunteers(systemStats.getVerifiedVolunteers() + countNewVolunteer);

        //count organization verified yesterday
        int countNewOrganization = organizationRepository.countCreatedBetween(start, end);
        systemStats.setVerifiedVolunteers(systemStats.getVerifiedOrganizations() + countNewOrganization);

        //get

        systemStatsRepository.save(systemStats);
    }

    //todo remove this after
    public void compileSystemStatsMonthly(int year, int month) {
//        //re compile verified volunteers, verifiedOrganization, counteventindomain
//        YearMonth prev = YearMonth.now().minusMonths(1);
//        OffsetDateTime startTimeOfMonth = prev.atDay(1).atStartOfDay(zone).toOffsetDateTime();
//        OffsetDateTime endTimeOfMonth = prev.atEndOfMonth().plusDays(1).atStartOfDay(zone).toOffsetDateTime();
//
//        //count volunteer verified in last month
//        int countNewVolunteer = volunteerRepository.countCreatedBetween(startTimeOfMonth, endTimeOfMonth);
//        int countNewOrganization = organizationRepository.countCreatedBetween(startTimeOfMonth, endTimeOfMonth);

        YearMonth yearMonth = YearMonth.of(year, month);

        List<OrganizationStats> organizationStats = organizationStatsRepository.findStatsBy(year, month);

        SystemStats systemStats = systemStatsRepository.findByYearAndMonth(year, month).orElseGet(() -> {
            SystemStats systemStat = new SystemStats();
            systemStat.setYear(year);
            systemStat.setMonth(month);
            return systemStat;
        });

        int completedEvents = 0;
        int creditHours = 0;
        int approvedApplications = 0;
        int attendedApplications = 0;
        for (OrganizationStats organizationStat : organizationStats) {
            completedEvents += organizationStat.getCompletedEvents();
            creditHours += organizationStat.getCreditHours();
            approvedApplications += organizationStat.getApprovedApplications();
            attendedApplications += organizationStat.getAttendedApplications();
        }
        systemStats.setCompletedEvents(completedEvents);
        systemStats.setCreditHours(creditHours);
        systemStats.setApprovedApplications(approvedApplications);
        systemStats.setAttendedApplications(attendedApplications);


        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        OffsetDateTime startTimeOfMonth = yearMonth.atDay(1).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endTimeOfMonth = yearMonth.atEndOfMonth().plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        //count volunteer, organization verified in last month
        int countNewVolunteer = volunteerRepository.countCreatedBetween(startTimeOfMonth, endTimeOfMonth);
        int countNewOrganization = organizationRepository.countCreatedBetween(startTimeOfMonth, endTimeOfMonth);

        systemStats.setVerifiedVolunteers(systemStats.getVerifiedVolunteers() + countNewVolunteer);
        systemStats.setVerifiedVolunteers(systemStats.getVerifiedOrganizations() + countNewOrganization);

        systemStatsRepository.save(systemStats);
    }
}
