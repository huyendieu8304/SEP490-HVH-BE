package com.sep490.g28.hvh.be.scheduler;

import com.sep490.g28.hvh.be.service.OrganizationStatsService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatsScheduler {
    OrganizationStatsService organizationStatsService;

    //4AM at the first day of month
    @Scheduled(cron = "0 0 4 1 * *", zone = "Asia/Ho_Chi_Minh")
    public void compileOrganizationsMonthlyStatistics() {

        //recalculate previous month's stats
        log.info("Start monthly stats compile cron job");
        organizationStatsService.compileOrganizationsMonthlyStatistics();

        log.info("Done monthly stats compile cron job");
    }
}
