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
//    @Scheduled(cron = "0 0 0 * * *")
//    public void endRecruiment() {
//        log.info("Start ending recruitment for event cron job");
//
//        //todo hcange the status to UPCOMING
//        eventService.completeEvent();
//
//        log.info("Done ending recruitment for event cron job");
//    }
//
//    @Scheduled(cron = "0 0 0 * * *")
//    public void startEvent() {
//        log.info("Start event cron job");
//
//        //todo, change the event status to ONGOING
//        eventService.completeEvent();
//
//        log.info("Done event cron job");
//    }
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
    @Scheduled(cron = "0 0 2 * * *")
    public void completeEvent() {
        log.info("Start completing event cron job");

        eventService.completeEvent();

        log.info("Done completing event cron job");
    }





}
