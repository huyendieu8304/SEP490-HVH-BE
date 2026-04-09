package com.sep490.g28.hvh.be.scheduler;

import com.sep490.g28.hvh.be.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusScheduler {
    EventService eventService;


//    //0AM
//    @Scheduled(cron = "0 0 0 * * *")
//    public void forceCheckOut() {
//        log.info("Start force check out for volunteer cron job");
//
//        //todo
//        // eventService.completeEvent();
//
//        log.info("Done force check out for volunteer cron job");
//    }
//
    @Scheduled(cron = "0 0 0 * * *")
    public void endRecruitment() {
        log.info("Start ending recruitment for event cron job");

        eventService.endRecruitment();

        log.info("Done ending recruitment for event cron job");
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void startEvents() {
        log.info("Start event cron job");

        //change the events status to ONGOING
        eventService.startEvents();

        log.info("Done event cron job");
    }

    //todo create check in code will run after startEvents
    //todo fix event SEssison, remove the not null constraint in check in code
//
//    @Scheduled(cron = "0 0 0 * * *")
//    public void endedEvent() {
//        log.info("Start event cron job");
//
//        //run after force check out
//        //todo, change the event status to ENDED
//        eventService.completeEvent();
//
//        log.info("Done event cron job");
//    }

    //2AM every day
//    @Scheduled(cron = "0 0 2 * * *")
    //todo enable this cron job
    public void completeEvents() {
        log.info("Start completing event cron job");

        eventService.completeEvents();

        log.info("Done completing event cron job");
    }





}
