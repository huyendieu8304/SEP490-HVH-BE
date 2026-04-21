package com.sep490.g28.hvh.be.scheduler;

import com.sep490.g28.hvh.be.service.EventService;
import com.sep490.g28.hvh.be.service.EventSessionService;
import com.sep490.g28.hvh.be.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusScheduler {
    EventService eventService;
    EventSessionService eventSessionService;
    OrganizationService organizationService;

    //0AM everyday
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void dailyEventJob() {
        runStep("endRecruitment", eventService::endRecruitment);
        runStep("endEvents", eventService::endEvents);
        runStep("startEvents", eventService::startEvents);
        runStep("createCheckIn", eventSessionService::createCheckInCode);
    }

    private void runStep(String name, Runnable step) {
        try {
            log.info("Start {}", name);
            step.run();
            log.info("Done {}", name);
        } catch (Exception e) {
            log.error("Fail {}", name, e);
            throw e; // stop flow
        }
    }

    //2AM every day
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void completeEvents() {
        log.info("Start completing events cron job");

        eventService.completeEvents();

        log.info("Done completing events cron job");
    }

    //4AM every day
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Ho_Chi_Minh")
    public void calculateOrganizationsAvgRating(){
        log.info("Start calculating organization avg rating cron job");
        organizationService.calculateOrganizationsAvgRating();
        log.info("Done calculating organization avg rating cron job");
    }



}
