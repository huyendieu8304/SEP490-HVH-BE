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

//    @Scheduled(cron = "0 0 0 * * *")
    public void endRecruitment() {
        log.info("Start ending recruitment for events cron job");

        eventService.endRecruitment();

        log.info("Done ending recruitment for events cron job");
    }

//    @Scheduled(cron = "0 0 0 * * *")
    public void startEvents() {
        log.info("Start start events cron job");

        //change the events status to ONGOING
        eventService.startEvents();

        log.info("Done start events cron job");
    }

    //todo create check in code will run after startEvents
    //todo fix event SEssison, remove the not null constraint in check in code
//
//    @Scheduled(cron = "0 0 0 * * *")
    public void endEvents() {
        log.info("Start end events cron job");

        //run after force check out
        //change the event status to ENDED
        eventService.endEvents();

        log.info("Done end events cron job");
    }

    //2AM every day
//    @Scheduled(cron = "0 0 2 * * *")
    //todo enable this cron job
    public void completeEvents() {
        log.info("Start completing events cron job");

        eventService.completeEvents();

        log.info("Done completing events cron job");
    }

    public void calculateOrganizationsAvgRating(){
        log.info("Start calculating organization avg rating cron job");
        organizationService.calculateOrganizationsAvgRating();
        log.info("Done calculating organization avg rating cron job");
    }


    public void createCheckInCode(){
        log.info("Start create check in code cron job");
        eventSessionService.createCheckInCode();
        log.info("Done create check in code cron job");
    }

}
