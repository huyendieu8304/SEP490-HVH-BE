package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventImagePayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventSessionPayload;
import com.sep490.g28.hvh.be.dto.event.request.*;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.event.request.SaveEventRequest;
import com.sep490.g28.hvh.be.dto.eventapplication.projection.EligibleApplicationProjection;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.*;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.mapper.EventMapper;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.*;
import com.sep490.g28.hvh.be.util.GeoUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventServiceImpl implements EventService {
    HostRepository hostRepository;
    ActivitySubDomainRepository activitySubDomainRepository;
    EventRepository eventRepository;
    VolunteerRepository volunteerRepository;
    VolunteerSavedEventRepository volunteerSavedEventRepository;
    OrganizationManagerRepository organizationManagerRepository;

    StorageService storageService;

    EventImageService eventImageService;
    EventSessionService eventSessionService;
    EventApplicationService eventApplicationService;
    OrganizationService organizationService;
    NotificationService notificationService;
    EmailService emailService;
    AuthService authService;
    CertificateService certificateService;
    VolunteerReviewService volunteerReviewService;

    CurrentUserProvider currentUserProvider;

    EventMapper eventMapper;
    private final EventApplicationRepository eventApplicationRepository;
    private final VolunteerReviewRepository volunteerReviewRepository;

    @Override
    public EventFeedResponse getEventFeeds(int pageNumber, int pageSize, boolean refresh,
                                           String name, String address, LocalDate startDate,
                                           LocalDate endDate, List<Short> activitySubDomains) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        //Get the slice based on the current action is refresh (swipe up) or load more (scroll end)
        Page<Event> page = null;

        //check if activitySubDomains is null or empty
        if(activitySubDomains == null || activitySubDomains.isEmpty()) {

            //If the action is refresh, get the slice within 1 hour ago
            if (refresh) {
                OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
                page = eventRepository.refresh(name, address, startDate, endDate, oneHourAgo, pageable);
                //Else if the action is load more, keep getting the slice with current searching params
            } else {
                page = eventRepository.search(name, address, startDate, endDate, pageable);
            }
        } else {
            if (refresh) {
                OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
                page = eventRepository.refreshWithActivitySubDomain(name, address, startDate, endDate, activitySubDomains,oneHourAgo, pageable);
            } else {
                page = eventRepository.searchWithActivitySubDomain(name, address, startDate, endDate, activitySubDomains, pageable);
            }
        }

        for(Event e : page.getContent()) {
            log.info("CONTENT OF SLICE: {a}" + e.getStatus());
        }


        //map the slice content (list of events) to EventSimpleResponse
        List<EventSimpleResponse> eventSimpleResponseList = Optional.of(page.getContent())
                .map(list -> list.stream()
//                        .filter(e -> e.getStatus().equals(EEventStatus.RECRUITING))
                        .map(e -> {

                                    String firstEventImageUrl = null;

                                    //get signed URL of file
                                    if (e.getImages() != null && !e.getImages().isEmpty()) {

                                        log.info("image of event: " + e.getImages());

                                        List<EventImage> eventImageList = e.getImages();

                                        CompletableFuture<String> firstEventImageFuture =
                                                storageService.getSignedUrlAsync(eventImageList.getFirst().getImagePath());

                                        try {
                                            CompletableFuture.allOf(firstEventImageFuture).join();
                                            firstEventImageUrl = firstEventImageFuture.join();
                                        } catch (CompletionException ex) {
                                            Throwable cause = ex.getCause();
                                            if (cause instanceof AppException ae) {
                                                //todo: handle app exception in viewEventFeeds
                                            } else {
                                                throw cause instanceof RuntimeException re ? re : ex;
                                            }
                                        }
                                    }

                                    return new EventSimpleResponse(
                                            e.getId(),
                                            e.getOrganization().getName(),
                                            e.getName(),
                                            firstEventImageUrl,
                                            e.getAddress(),
                                            e.getStartDate(),
                                            e.getRecruitmentEndDate()
                                    );
                                }

                        ).toList())
                .orElse(Collections.emptyList());

        // If after load the page with n size,
        // and page.hasNext() is true (the slice will auto check this)
        // , move the cursor to the next page, which will load more content
        // (equivalent to call the api one more time)
        return new EventFeedResponse(
                eventSimpleResponseList,
                page.hasNext() ? String.valueOf(pageNumber + 1) : null,
                page.hasNext()
        );
    }

    /*
        draft -> create
               -> edit
        submit -> create
                -> edit
         */

    @Transactional
    public EditEventResponse draftEvent(EditEventRequest request) {
        if (request.getEventId() != null) {
            //event has been saved as drafted
            //get event from db
            Event event = eventRepository.findById(request.getEventId()).orElseThrow(
                    () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
            );
            //todo check host of event
            return editEvent(request, event, EEventStatus.EDITING);
        } else {
            return createEvent(request, EEventStatus.EDITING);
        }
    }

    @Transactional
    public EditEventResponse submitEvent(EditEventRequest request) {
        if (request.getEventId() != null) {
            //event has been saved as drafted
            //get event from db
            Event event = eventRepository.findById(request.getEventId()).orElseThrow(
                    () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
            );
            //todo check host of event
            return editEvent(request, event, EEventStatus.SUBMITTED);
        } else {
            return createEvent(request, EEventStatus.SUBMITTED);
        }
    }

    private EditEventResponse createEvent(EditEventRequest request, EEventStatus eventStatus) {

        Event event = new Event();
        EditEventResponse response = new EditEventResponse();
        //the event is completely new

        ActivitySubDomain activitySubDomain = activitySubDomainRepository.findById(request.getActivitySubDomainId())
                .orElseThrow(() -> new AppException(ActivityDomainErrorCode.SUBDOMAIN_NOT_EXISTED));
        event.setActivitySubDomain(activitySubDomain);

        Host host = hostRepository.getReferenceById(currentUserProvider.getId());
        Organization organization = host.getOrganization();

        event.setHost(host);
        event.setCreateBy(host);
        event.setOrganization(organization);

        event.setRecruitmentEndDate(request.getRecruitmentEndDate());
        eventSessionService.addEventSessionsForCreateEvent(
                event,
                request.getEventSessions()
        );

        //set event's information
        mapEventSimpleField(request, event);

        event.setStatus(eventStatus);
        //save event
        event = eventRepository.save(event);
        log.info("Event is created: eventId={}", event.getId());


        //adding images
        List<String> uploadUrls = eventImageService.addEventImages(event, request.getUpdateImages());
        response.setUploadUrls(uploadUrls);

        //after finish all things to do with repo or other services, send notification to host if the event is submitted
        if (eventStatus.equals(EEventStatus.SUBMITTED)) {
            log.info("Event is submitted: eventId={}", event.getId());
            notificationService.sendEventCreatedNotification(event, event.getHost());
        }
        return response;
    }

    private EditEventResponse editEvent(EditEventRequest request, Event event, EEventStatus eventStatus) {
        EditEventResponse response = new EditEventResponse();
        if (!EEventStatus.canEventBeEdited(event.getStatus()))
            //event is not edit table
            throw new AppException(EventErrorCode.EVENT_NOT_EDITABLE);

        //event ís editable
        //edit event's images
        List<String> uploadUrls = eventImageService.updateEventImages(event, request.getUpdateImages());
        response.setUploadUrls(uploadUrls);

        //edit EventDateTime
        ActivitySubDomain activitySubDomain = activitySubDomainRepository.findById(request.getActivitySubDomainId())
                .orElseThrow(() -> new AppException(ActivityDomainErrorCode.SUBDOMAIN_NOT_EXISTED));
        event.setActivitySubDomain(activitySubDomain);

        event.setRecruitmentEndDate(request.getRecruitmentEndDate());
        eventSessionService.updateEventSessions(
                event,
                request.getEventSessions()
        );

        //set event's information
        mapEventSimpleField(request, event);
        event.setStatus(eventStatus);

        //save event
        eventRepository.save(event);
        log.info("Event is edited: eventId={}", event.getId());

        //after finish all things to do with repo or other services, send notification to host if the event is submitted
        if (eventStatus.equals(EEventStatus.SUBMITTED)) {
            log.info("Event is submitted: eventId={}", event.getId());
            notificationService.sendEventCreatedNotification(event, event.getHost());
        }
        return response;
    }

    private void mapEventSimpleField(EditEventRequest request, Event event) {
         //check in place
        Point checkInLocation = GeoUtils.toPoint(request.getCheckInPlaceLat(), request.getCheckInPlaceLng());
        event.setCheckInLocation(checkInLocation);
        event.setCheckInAccuracyMeters((double) request.getCheckInPlaceAccuracyMeters());

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setAddress(request.getAddress());
        event.setDetailAddress(request.getDetailAddress());

        event.setAutoApprove(request.getAutoApprove());
        event.setServingActivity(request.getServingActivity());
        event.setServedTarget(request.getServedTarget());
        event.setServingPlaceType(request.getServingPlaceType());
    }

    @Override
    public EventDetailsResponse getEventDetails(UUID id) {
        //check id exist
        Event event = eventRepository.findById(id).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //get signed URL of file
        List<CompletableFuture<String>> imagesFutures = new ArrayList<>();
        if (event.getImages() != null) {
            List<EventImage> imagesList = event.getImages();
            for (EventImage image : imagesList) {
                CompletableFuture<String> imageFuture =
                        storageService.getSignedUrlAsync(image.getImagePath());
                imagesFutures.add(imageFuture);
            }
        }

        List<String> imagesUrls = new ArrayList<>();
        try {

            CompletableFuture.allOf(imagesFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<String> imageFuture : imagesFutures) {
                imagesUrls.add(imageFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo: handle exception at getEventDetails
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        String hostPhone = "";
        if (event.getHost() != null) {
            hostPhone = event.getHost().getPhone();
        }

        String orgName = "";
        if (event.getOrganization() != null) {
            orgName = event.getOrganization().getName();
        }

        String activitySubDomainName = "";
        if (event.getActivitySubDomain() != null) {
            activitySubDomainName = event.getActivitySubDomain().getName();
        }

        //Map event sessions to response
        List<EventSessionDetailsResponse> eventSessions = event.getSessions().stream()
                .map(es -> new EventSessionDetailsResponse(
                        es.getId(),
                        es.getStartDateTime(),
                        es.getEndDateTime(),
                        es.getExpectedVolAmount(),
                        es.getExpectedSerAmount(),
                        es.getApprovedApplicationCount()
                )).toList();

        //get lat and lng of check in location
        Double lat = 0.0;
        Double lng = 0.0;

        if(event.getCheckInLocation() != null) {
            lat = GeoUtils.getLat(event.getCheckInLocation());
            lng = GeoUtils.getLng(event.getCheckInLocation());
        }

        return EventDetailsResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .imageUrls(imagesUrls)
                .description(event.getDescription())
                .address(event.getAddress())
                .detailAddress(event.getDetailAddress())
                .activitySubDomain(activitySubDomainName)
                .servedTarget(event.getServedTarget())
                .servingPlaceType(event.getServingPlaceType())
                .startDate(event.getStartDate())
                .recruitmentEndDate(event.getRecruitmentEndDate())
                .latCheckInLocation(lat)
                .lngCheckInLocation(lng)
                .checkInAccuracyMeters(event.getCheckInAccuracyMeters())
                .hostPhone(hostPhone)
                .orgName(orgName)
                .eventSessions(eventSessions)
                .build();
    }

    //todo
    @Override
    public void saveEvent(SaveEventRequest request) {

        UUID volunteerId = currentUserProvider.getId();
        UUID eventId = UUID.fromString(request.getEventId());


        //get volunteer from id
        Volunteer volunteer = volunteerRepository.findById(volunteerId).orElseThrow(
                () -> new AppException(VolunteerErrorCode.VOLUNTEER_NOT_EXISTED)
        );


        //get event from id
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //create volunteerSavedEvent in db
        VolunteerSavedEvent volunteerSavedEvent = new VolunteerSavedEvent();
        volunteerSavedEvent.setVolunteer(volunteer);
        volunteerSavedEvent.setEvent(event);

        volunteerSavedEventRepository.save(volunteerSavedEvent);
    }


    private void applyNonCriticalUpdateEvent(Event event, UpdateEventPayload payload) {
        //apply images
        if (payload.getEventImages() != null) {
            List<EventImage> oldImages = event.getImages();
            List<UpdateEventImagePayload> newImages = payload.getEventImages();
            event.setImages(eventImageService.resolveUpdatedEventImages(event, oldImages, newImages));
        }

        if (payload.getDescription() != null) {
            event.setDescription(payload.getDescription());
        }

        if (payload.getAutoApprove() != null) {
            event.setAutoApprove(payload.getAutoApprove());
        }

        if (payload.getServingPlaceType() != null) {
            event.setServingPlaceType(payload.getServingPlaceType());
        }
    }

    private void applyCriticalUpdateEvent(Event event, UpdateEventPayload payload) {
        if (payload.getEventSessions() != null) {

            List<EventSession> oldSessions = event.getSessions();
            List<UpdateEventSessionPayload> newSessions = payload.getEventSessions();
            List<EventSession> updatedEventSessions = eventSessionService.resolveUpdateEventSessions(event, oldSessions, newSessions);

            //check whether the host is hosting multiple event session in a day if the update is applied?
            List<EventSession> conflictSession =  eventSessionService.findConflictSessionDateOfHost(
                    event.getHost().getId(),
                    event.getId(),
                    updatedEventSessions
            );
            if (!conflictSession.isEmpty()) {
                throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
            }

            event.setSessions(updatedEventSessions);
            event.setStartDate(payload.getStartDate());
            event.setEndDate(payload.getEndDate());

        }
        if (payload.getAddress() != null) {
            event.setAddress(payload.getAddress());
        }
        if (payload.getDetailAddress() != null) {
            event.setDetailAddress(payload.getDetailAddress());
        }
        if (payload.getRecruitmentEndDate() != null) {
            event.setRecruitmentEndDate(payload.getRecruitmentEndDate());
        }
        if (payload.getCheckInLocationLat() != null && payload.getCheckInLocationLng() != null) {
            event.setCheckInLocation(GeoUtils.toPoint(payload.getCheckInLocationLat(), payload.getCheckInLocationLng()));
        }
        if (payload.getCheckInLocationAccuracyMeters() != null) {
            event.setCheckInAccuracyMeters(payload.getCheckInLocationAccuracyMeters());
        }
    }

    @Override
    public void approveEventByManager(UUID eventId) {
        //get event out from repo
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check event status
        if (!event.getStatus().equals(EEventStatus.SUBMITTED)) {
            throw new AppException(EventErrorCode.ACTION_NOT_EXECUTABLE);
        }

        //Is this event being created or being updated
        if (event.getUpdateCritical() == null) {
            //the manager is approving for a create request
            //approve time must not pass recruitment end date
            LocalDate today = LocalDate.now();
            if (today.isAfter(event.getRecruitmentEndDate())){
                throw new AppException(EventErrorCode.EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE);
            }

            //check whether the host is hosting multiple event session in a day or not?
            List<EventSession> conflictSession =  eventSessionService.findConflictSessionDateOfHost(
                    event.getHost().getId(),
                    eventId,
                    event.getSessions()
            );
            if (!conflictSession.isEmpty()) {
                throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
            }

            //update in db
            event.setStatus(EEventStatus.APPROVED_BY_MNG);
            eventRepository.save(event);

            //send notification
            notificationService.sendEventCreateApprovedByOrgManagerNotification(event);
            log.info("Event creation is approved by Organization Manager: eventId={}", event.getId());

        } else if (Boolean.TRUE.equals(event.getUpdateCritical())) {
            //the manager is approving for an update CRITICAL information request
            //approve time must not pass recruitment end date
            LocalDate today = LocalDate.now();
            LocalDate newRecruitmentEndDate =
                    event.getUpdateEventPayload().getRecruitmentEndDate() == null
                            ? event.getRecruitmentEndDate()
                            : event.getUpdateEventPayload().getRecruitmentEndDate();
            if (today.isAfter(newRecruitmentEndDate)){
                throw new AppException(EventErrorCode.EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE);
            }

            if (event.getUpdateEventPayload().getEventSessions() != null) {

                List<EventSession> newSessions = event.getUpdateEventPayload().getEventSessions().stream()
                        .map(s -> {
                            EventSession session = new EventSession();
                            session.setId(s.getId());
                            session.setStartDateTime(s.getStartDateTime());
                            session.setEndDateTime(s.getEndDateTime());
                            return session;
                        }).toList();
                //check whether the host is hosting multiple event session in a day if the update is applied?
                List<EventSession> conflictSession = eventSessionService.findConflictSessionDateOfHost(
                        event.getHost().getId(),
                        event.getId(),
                        newSessions
                );
                if (!conflictSession.isEmpty()) {
                    throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
                }
            }

            //update in db
            event.setStatus(EEventStatus.APPROVED_BY_MNG);
            eventRepository.save(event);

            //send notification to host
            notificationService.sendEventUpdateCriticalApprovedByOrgManagerNotification(event.getHost().getId(), event);

            log.info("Event update (critical) is approved by Organization Manager: eventId={}", event.getId());
        } else {
            //the manager is approving for an update NON-CRITICAL information request
            //approve time must not pass recruitment end date
            LocalDate today = LocalDate.now();
            if (today.isAfter(event.getRecruitmentEndDate())){
                throw new AppException(EventErrorCode.EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE);
            }

            //set the event status back to RECRUITING
            event.setStatus(EEventStatus.RECRUITING);

            //apply update information
            applyNonCriticalUpdateEvent(event, event.getUpdateEventPayload());

            //remove update information and update critical
            event.setUpdateCritical(null);
            event.setUpdateEventPayload(null);

            //save event
            eventRepository.save(event);

            //send notification to host
            notificationService.sendEventUpdateNonCriticalApprovedByOrgManagerNotification(event.getHost().getId(), event);

            //send notification to applied volunteers to inform about the change (both PENDING and APPROVED)
            List<UUID> sessionIds = event.getSessions().stream().map(EventSession::getId).toList();
            List<EventApplication> applications = eventApplicationRepository.getPendingAndApprovedApplications(sessionIds);
            notificationService.sendEventUpdateNonCriticalApprovedByOrgManagerNotification(applications, event.getName());

            log.info("Event update (non-critical) is approved by Organization Manager: eventId={}", event.getId());
        }
    }



    @Override
    public void rejectEventByManager(UUID eventId, RejectEventRequest request) {
        //get event out from repo
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check event status
        if (!event.getStatus().equals(EEventStatus.SUBMITTED)) {
            throw new AppException(EventErrorCode.ACTION_NOT_EXECUTABLE);
        }

        //Is this event being created or being updated
        if (event.getUpdateCritical() == null) {
            //the manager is rejecting a create request
            //update in db
            event.setStatus(EEventStatus.REJECTED_BY_MNG);
            eventRepository.save(event);

            //send notification
            notificationService.sendEventCreateRejectedByOrgManagerNotification(event, request.getReason());
            log.info("Event creation is rejected by Organization Manager: eventId={}", event.getId());
        } else {
            //the manager is rejecting a update request
            //set the event status to  the real status according to time
            LocalDate today = LocalDate.now();
            if (!today.isAfter(event.getRecruitmentEndDate())){
                event.setStatus(EEventStatus.RECRUITING);
            } else if (today.isBefore(event.getStartDate())){
                event.setStatus(EEventStatus.UPCOMING);
            } else if (today.isBefore(event.getEndDate())){
                event.setStatus(EEventStatus.ONGOING);
            } else {
                event.setStatus(EEventStatus.COMPLETED);
            }

            //remove update information and update critical
            event.setUpdateCritical(null);
            event.setUpdateEventPayload(null);

            //save event
            eventRepository.save(event);

            //send notification to host to inform about the rejection
            notificationService.sendEventUpdateRejectedByOrgManagerNotification(event.getHost().getId(), event);

            log.info("Event update is rejected by Organization Manager: eventId={}", event.getId());
        }
    }

    @Override
    public void approveEventByAdmin(UUID eventId) {
        //get event out from repo
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check event status
        if (!event.getStatus().equals(EEventStatus.APPROVED_BY_MNG)) {
            throw new AppException(EventErrorCode.ACTION_NOT_EXECUTABLE);
        }

        //Is this event being created or being updated
        if (event.getUpdateCritical() == null) {
            //admin approving create request
            //approve time must not pass recruitment end date
            LocalDate today = LocalDate.now();
            if (today.isAfter(event.getRecruitmentEndDate())){
                throw new AppException(EventErrorCode.EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE);
            }

            //check whether the host is hosting multiple event session in a day or not?
            List<EventSession> conflictSession =  eventSessionService.findConflictSessionDateOfHost(
                    event.getHost().getId(),
                    eventId,
                    event.getSessions()
            );
            if (!conflictSession.isEmpty()) {
                throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
            }

            //update in db
            event.setStatus(EEventStatus.RECRUITING);
            eventRepository.save(event);
            log.info("Event creation is approved by System Admin: eventId={}", event.getId());

            //send notification
            notificationService.sendEventCreateApprovedByAdminNotification(event);
        } else {
            //the manager is approving for an update CRITICAL information request
            //approve time must not pass recruitment end date
            LocalDate today = LocalDate.now();
            LocalDate newRecruitmentEndDate =
                    event.getUpdateEventPayload().getRecruitmentEndDate() == null
                            ? event.getRecruitmentEndDate()
                            : event.getUpdateEventPayload().getRecruitmentEndDate();
            if (today.isAfter(newRecruitmentEndDate)){
                throw new AppException(EventErrorCode.EVENT_APPROVE_TIME_PASS_RECRUITMENT_END_DATE);
            }

            //set the event status back to RECRUITING
            event.setStatus(EEventStatus.RECRUITING);

            //apply update information
            applyCriticalUpdateEvent(event, event.getUpdateEventPayload());
            applyNonCriticalUpdateEvent(event, event.getUpdateEventPayload());

            //remove update information and update critical
            event.setUpdateCritical(null);
            event.setUpdateEventPayload(null);

            //save event
            eventRepository.save(event);

            //cancel all application that are PENDING or APPROVED
            List<EventApplication> applications = eventApplicationService.cancelAllApplicationsOfEvent(event);

            //send notification to host and org mng to inform about the approval
            notificationService.sendEventUpdateCriticalApprovedByAdminNotification(event);

            //send notification to applied volunteers to inform about the change (both PENDING and APPROVED)
            notificationService.sendEventUpdateCriticalApprovedByAdminNotification(applications, event.getName());

            log.info("Event update is approved by System Admin: eventId={}", event.getId());
        }
    }

    @Override
    public void rejectEventByAdmin(UUID eventId, RejectEventRequest request) {
        //get event out from repo
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check event status
        if (!event.getStatus().equals(EEventStatus.APPROVED_BY_MNG)) {
            throw new AppException(EventErrorCode.ACTION_NOT_EXECUTABLE);
        }

        //Is this event being created or being updated
        if (event.getUpdateCritical() == null) {
            //the admin is rejecting a create request
            //update in db
            event.setStatus(EEventStatus.REJECTED_BY_AD);
            eventRepository.save(event);

            //send notification
            notificationService.sendEventCreateRejectedByAdminNotification(event, request.getReason());
            log.info("Event creation is rejected by System Admin: eventId={}", event.getId());
        } else {
            //the admin is rejecting an update request
            //set the event status to  the real status according to time
            LocalDate today = LocalDate.now();
            if (!today.isAfter(event.getRecruitmentEndDate())){
                event.setStatus(EEventStatus.RECRUITING);
            } else if (today.isBefore(event.getStartDate())){
                event.setStatus(EEventStatus.UPCOMING);
            } else if (today.isBefore(event.getEndDate())){
                event.setStatus(EEventStatus.ONGOING);
            } else {
                event.setStatus(EEventStatus.COMPLETED);
            }

            //remove update information and update critical
            event.setUpdateCritical(null);
            event.setUpdateEventPayload(null);

            //save event
            eventRepository.save(event);

            //send notification to host and manager to inform about the rejection
            notificationService.sendEventUpdateCriticalRejectedByAdminNotification(event);

            log.info("Event update is rejected by System Admin: eventId={}", event.getId());
        }

    }

    //todo unit test for this method
    @Override
    public Page<EventSimpleResponseForManager> getPendingEventsByManager(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "created_at")
        );
        UUID managerId = currentUserProvider.getId();
        OrganizationManager manager = organizationManagerRepository.getReferenceById(managerId);
        Organization organization = manager.getOrganization();

        List<String> pendingStatus = Stream.of(
                EEventStatus.SUBMITTED,
                EEventStatus.APPROVED_BY_MNG,
                EEventStatus.REJECTED_BY_MNG,
                EEventStatus.REJECTED_BY_AD
        ).map(Enum::name).toList();

        return eventRepository.findEventsByOrganizationIdAnd(
                organization.getId(),
                pendingStatus,
                eventName,
                pageable
        ).map(eventMapper::toEventSimpleResponseForManager);
    }

    //todo unit test for this method
    @Override
    public Page<EventSimpleResponseForManager> getApprovedEventsByManager(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "created_at")
        );
        UUID managerId = currentUserProvider.getId();
        OrganizationManager manager = organizationManagerRepository.getReferenceById(managerId);
        Organization organization = manager.getOrganization();

        List<String> approvedStatus = Stream.of(
                EEventStatus.RECRUITING,
                EEventStatus.UPCOMING,
                EEventStatus.ONGOING,
                EEventStatus.UPCOMING,
                EEventStatus.ENDED,
                EEventStatus.COMPLETED,
                EEventStatus.CANCELLED
        ).map(Enum::name).toList();

        return eventRepository.findEventsByOrganizationIdAnd(
                organization.getId(),
                approvedStatus,
                eventName,
                pageable
        ).map(eventMapper::toEventSimpleResponseForManager);
    }

    //todo unit test for this method
    @Override
    public Page<EventSimpleResponseForAdmin> getPendingEventsByAdmin(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "created_at")
        );

        List<String> pendingStatus = Stream.of(
                EEventStatus.APPROVED_BY_MNG,
                EEventStatus.REJECTED_BY_AD
        ).map(Enum::name).toList();

        return eventRepository.findEventsByAdminAnd(
                pendingStatus,
                eventName,
                pageable
        ).map(eventMapper::toEventSimpleResponseForAdmin);
    }

    //todo unit test for this method
    @Override
    public Page<EventSimpleResponseForAdmin> getRunningEventsByAdmin(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "created_at")
        );

        List<String> runningStatus = Stream.of(
                EEventStatus.RECRUITING,
                EEventStatus.UPCOMING,
                EEventStatus.ONGOING,
                EEventStatus.UPCOMING,
                EEventStatus.ENDED
        ).map(Enum::name).toList();

        return eventRepository.findEventsByAdminAnd(
                runningStatus,
                eventName,
                pageable
        ).map(eventMapper::toEventSimpleResponseForAdmin);
    }

    @Override
    public EventDetailsResponseForManager getEventDetailsByManager(UUID id) {
        //check id exist
        Event event = eventRepository.findById(id).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //get regular information of event
        EEventStatus eventStatus = event.getStatus();

        //get signed URL of file
        List<CompletableFuture<String>> imagesFutures = new ArrayList<>();
        if (event.getImages() != null) {
            List<EventImage> imagesList = event.getImages();
            for (EventImage image : imagesList) {
                CompletableFuture<String> imageFuture =
                        storageService.getSignedUrlAsync(image.getImagePath());
                imagesFutures.add(imageFuture);
            }
        }

        List<String> imagesUrls = new ArrayList<>();
        try {

            CompletableFuture.allOf(imagesFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<String> imageFuture : imagesFutures) {
                imagesUrls.add(imageFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo: handle exception at getEventDetails
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        UUID hostId = null;
        String hostPhone = "";
        String hostEmail = "";
        String hostName = "";
        if (event.getHost() != null) {
            hostId = event.getHost().getId();
            hostPhone = event.getHost().getPhone();
            hostEmail = event.getHost().getEmail();
            hostName = event.getHost().getFullName();
        }

        String activitySubDomainName = "";
        if (event.getActivitySubDomain() != null) {
            activitySubDomainName = event.getActivitySubDomain().getName();
        }

        //Map event sessions to response
        List<EventSessionDetailsResponse> eventSessions = event.getSessions().stream()
                .map(es -> new EventSessionDetailsResponse(
                        es.getId(),
                        es.getStartDateTime(),
                        es.getEndDateTime(),
                        es.getExpectedVolAmount(),
                        es.getExpectedSerAmount(),
                        es.getApprovedApplicationCount()
                )).toList();

        //check whether the host is hosting other event or not?
        List<EventSession> conflictSession =
                eventSessionService.findConflictSessionDateOfHost(
                        event.getHost().getId(),
                        id,
                        event.getSessions()
                );

        List<EventSessionDetailsResponse> conflictSessions = Optional.of(conflictSession)
                .map(cs -> cs.stream()
                        .map(es -> new EventSessionDetailsResponse(
                                es.getId(),
                                es.getStartDateTime(),
                                es.getEndDateTime(),
                                es.getExpectedVolAmount(),
                                es.getExpectedSerAmount(),
                                es.getApprovedApplicationCount()
                        )).toList()).orElse(Collections.emptyList());;

        if (!conflictSession.isEmpty()) {
            note.append(EventErrorCode.DUPLICATE_HOSTED_DATE.getMessage()).append("\n");
        }

        //get lat and lng of check in location
        Double lat = 0.0;
        Double lng = 0.0;

        if(event.getCheckInLocation() != null) {
            lat = GeoUtils.getLat(event.getCheckInLocation());
            lng = GeoUtils.getLng(event.getCheckInLocation());
        }

        LocalDate startDate = null;
        LocalDate recruitmentEndDate = null;

        //get specified info of SUMMITED status
//        if(eventStatus.toString().equals("SUBMITTED")){
//            startDate = event.getStartDate();
//            recruitmentEndDate = event.getRecruitmentEndDate();
//        }

        //get specified info of RECRUITING status
//        if(eventStatus.toString().equals("RECRUITING")){
        startDate = event.getStartDate();
        recruitmentEndDate = event.getRecruitmentEndDate();
//        }

        return EventDetailsResponseForManager.builder()
                .id(event.getId())
                .name(event.getName())
                .imageUrls(imagesUrls)
                .description(event.getDescription())
                .address(event.getAddress())
                .detailAddress(event.getDetailAddress())
                .activitySubDomain(activitySubDomainName)
                .servedTarget(event.getServedTarget())
                .servingPlaceType(event.getServingPlaceType())
                .startDate(startDate)
                .recruitmentEndDate(recruitmentEndDate)
                .autoApprove(event.isAutoApprove())
                .latCheckInLocation(lat)
                .lngCheckInLocation(lng)
                .checkInAccuracyMeters(event.getCheckInAccuracyMeters())
                .createdAt(event.getCreatedAt())
                .hostId(hostId)
                .hostPhone(hostPhone)
                .hostEmail(hostEmail)
                .hostName(hostName)
                .status(eventStatus)
                .eventSessions(eventSessions)
                .conflictSessions(conflictSessions)
                .note(note.toString())
                .build();
    }

    @Override
    public EventDetailsResponseForSystemAdmin getEventDetailsBySystemAdmin(UUID id) {
        //check id exist
        Event event = eventRepository.findById(id).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //get regular information of event
        EEventStatus eventStatus = event.getStatus();

        //get signed URL of file
        List<CompletableFuture<String>> imagesFutures = new ArrayList<>();
        if (event.getImages() != null) {
            List<EventImage> imagesList = event.getImages();
            for (EventImage image : imagesList) {
                CompletableFuture<String> imageFuture =
                        storageService.getSignedUrlAsync(image.getImagePath());
                imagesFutures.add(imageFuture);
            }
        }

        List<String> imagesUrls = new ArrayList<>();
        try {

            CompletableFuture.allOf(imagesFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<String> imageFuture : imagesFutures) {
                imagesUrls.add(imageFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo: handle exception at getEventDetails
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }


        String hostPhone = "";
        if (event.getHost() != null) {
            hostPhone = event.getHost().getPhone();
        }


        String activitySubDomainName = "";
        if (event.getActivitySubDomain() != null) {
            activitySubDomainName = event.getActivitySubDomain().getName();
        }

        //Map event sessions to response
        List<EventSessionDetailsResponse> eventSessions = event.getSessions().stream()
                .map(es -> new EventSessionDetailsResponse(
                        es.getId(),
                        es.getStartDateTime(),
                        es.getEndDateTime(),
                        es.getExpectedVolAmount(),
                        es.getExpectedSerAmount(),
                        es.getApprovedApplicationCount()
                )).toList();

        //check whether the host is hosting other event or not?
        List<EventSession> conflictSession =
                eventSessionService.findConflictSessionDateOfHost(
                        event.getHost().getId(),
                        id,
                        event.getSessions()
                );

        List<EventSessionDetailsResponse> conflictSessions = Optional.of(conflictSession)
                .map(cs -> cs.stream()
                        .map(es -> new EventSessionDetailsResponse(
                                es.getId(),
                                es.getStartDateTime(),
                                es.getEndDateTime(),
                                es.getExpectedVolAmount(),
                                es.getExpectedSerAmount(),
                                es.getApprovedApplicationCount()
                        )).toList()).orElse(Collections.emptyList());;

        if (!conflictSession.isEmpty()) {
            note.append(EventErrorCode.DUPLICATE_HOSTED_DATE.getMessage()).append("\n");
        }

        //get lat and lng of check in location
        Double lat = 0.0;
        Double lng = 0.0;

        if(event.getCheckInLocation() != null) {
            lat = GeoUtils.getLat(event.getCheckInLocation());
            lng = GeoUtils.getLng(event.getCheckInLocation());
        }

        LocalDate startDate = null;
        LocalDate recruitmentEndDate = null;

        startDate = event.getStartDate();
        recruitmentEndDate = event.getRecruitmentEndDate();

        return EventDetailsResponseForSystemAdmin.builder()
                .id(event.getId())
                .name(event.getName())
                .imageUrls(imagesUrls)
                .description(event.getDescription())
                .address(event.getAddress())
                .detailAddress(event.getDetailAddress())
                .activitySubDomain(activitySubDomainName)
                .servedTarget(event.getServedTarget())
                .servingPlaceType(event.getServingPlaceType())
                .startDate(startDate)
                .recruitmentEndDate(recruitmentEndDate)
                .autoApprove(event.isAutoApprove())
                .latCheckInLocation(lat)
                .lngCheckInLocation(lng)
                .checkInAccuracyMeters(event.getCheckInAccuracyMeters())
                .createdAt(event.getCreatedAt())
                .hostPhone(hostPhone)
                .status(eventStatus)
                .eventSessions(eventSessions)
                .conflictSessions(conflictSessions)
                .note(note.toString())
                .build();
    }

    @Override
    public Page<EventSimpleResponseForHost> getEventsByHost(int pageNumber, int pageSize, String eventName, String inputStatus) {
        //get current logged in host Id
        UUID hostId = currentUserProvider.getId();

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        EEventStatus status =
                (inputStatus == null || inputStatus.isBlank())
                        ? null
                        : EEventStatus.valueOf(inputStatus);

        Page<Event> events = eventRepository.findEventsByHostId(hostId, status, eventName, pageable);

        //check if there's no event with input status
        if(events.getContent().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, events.getTotalElements());
        }

        //map Event to EventSimpleResponseForHost
        return events.map(e -> {

            String firstEventImageUrl = null;

            //get signed URL of file
            if (e.getImages() != null && !e.getImages().isEmpty()) {

                List<EventImage> eventImageList = e.getImages();

                CompletableFuture<String> firstEventImageFuture =
                        storageService.getSignedUrlAsync(eventImageList.getFirst().getImagePath());

                try {
                    CompletableFuture.allOf(firstEventImageFuture).join();
                    firstEventImageUrl = firstEventImageFuture.join();
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AppException ae) {
                        //todo: handle app exception in viewEventFeeds
                    } else {
                        throw cause instanceof RuntimeException re ? re : ex;
                    }
                }
            }

            return new EventSimpleResponseForHost(
                    e.getId(),
                    e.getName(),
                    firstEventImageUrl,
                    e.getAddress(),
                    e.getStartDate(),
                    e.getRecruitmentEndDate(),
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        });
    }

    @Override
    public EventDetailsResponseForHost getEventDetailsByHost(UUID id) {
        //check id exist
        Event event = eventRepository.findById(id).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //todo check if event belongs to host

        StringBuilder note = new StringBuilder();

        //get regular information of event
        EEventStatus eventStatus = event.getStatus();

        //get signed URL of file
        List<CompletableFuture<String>> imagesFutures = new ArrayList<>();
        if (event.getImages() != null) {
            List<EventImage> imagesList = event.getImages();
            for (EventImage image : imagesList) {
                CompletableFuture<String> imageFuture =
                        storageService.getSignedUrlAsync(image.getImagePath());
                imagesFutures.add(imageFuture);
            }
        }

        List<String> imagesUrls = new ArrayList<>();
        try {

            CompletableFuture.allOf(imagesFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<String> imageFuture : imagesFutures) {
                imagesUrls.add(imageFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo: handle exception at getEventDetails
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        String activitySubDomainName = "";
        if (event.getActivitySubDomain() != null) {
            activitySubDomainName = event.getActivitySubDomain().getName();
        }

        //Map event sessions to response
        List<EventSessionDetailsResponse> eventSessions = event.getSessions().stream()
                .map(es -> new EventSessionDetailsResponse(
                        es.getId(),
                        es.getStartDateTime(),
                        es.getEndDateTime(),
                        es.getExpectedVolAmount(),
                        es.getExpectedSerAmount(),
                        es.getApprovedApplicationCount()
                )).toList();

        //get lat and lng of check in location
        Double lat = 0.0;
        Double lng = 0.0;

        if(event.getCheckInLocation() != null) {
            lat = GeoUtils.getLat(event.getCheckInLocation());
            lng = GeoUtils.getLng(event.getCheckInLocation());
        }

        LocalDate startDate = null;
        LocalDate recruitmentEndDate = null;

        startDate = event.getStartDate();
        recruitmentEndDate = event.getRecruitmentEndDate();

        return EventDetailsResponseForHost.builder()
                .id(event.getId())
                .name(event.getName())
                .imageUrls(imagesUrls)
                .description(event.getDescription())
                .address(event.getAddress())
                .detailAddress(event.getDetailAddress())
                .servingActivity(event.isServingActivity())
                .activitySubDomain(activitySubDomainName)
                .servedTarget(event.getServedTarget())
                .servingPlaceType(event.getServingPlaceType())
                .startDate(startDate)
                .recruitmentEndDate(recruitmentEndDate)
                .autoApprove(event.isAutoApprove())
                .latCheckInLocation(lat)
                .lngCheckInLocation(lng)
                .checkInAccuracyMeters(event.getCheckInAccuracyMeters())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .status(eventStatus)
                .eventSessions(eventSessions)
                .note(note.toString())
                .build();
    }

    @Override
    public void announceVolunteersOfEvent(UUID eventId, AnnounceVolunteerRequest request) {
        //find the event
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //host can only send notification to registered Volunteer when the event is in status UPCOMING and ONGOING
        if (!(event.getStatus().equals(EEventStatus.UPCOMING) || event.getStatus().equals(EEventStatus.ONGOING))){
            throw new AppException(EventErrorCode.EVENT_ANNOUNCEMENT_CANNOT_SENT);
        }

        //send notification
        notificationService.sendNotificationToVolunteersOfEvent(event.getId(), request);
    }

    @Transactional
    @Override
    public void cancelEventByHost(UUID eventId, CancelEventRequest request) {
        //find the event
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check the event status cancelable?
        if (!EEventStatus.canEventBeCancelled(event.getStatus())) {
            throw new AppException(EventErrorCode.EVENT_CANNOT_CANCELLED);
        }

        //update event status to cancelled
        event.setStatus(EEventStatus.CANCELLED);
        eventRepository.save(event);

        //deduct the credit hour of the organization by 3
        Organization organization = event.getOrganization();
        organizationService.deductCreditHourOfOrganization(organization, 3);

        //cancel all applications of volunteer to the event
        List<EventApplication> eventApplications = eventApplicationService.cancelAllApplicationsOfEvent(event);

        //send notification to all the volunteer that applied to the event
        notificationService.sentEventCancelledByHostNotification(eventApplications, event.getName(), request.getReason());

        //send email to the org manager
        OrganizationManager manager = organization.getOrganizationManager();
        Host host = event.getHost();
        emailService.sendEventCancelledByHostEmail(
                manager.getEmail(),
                manager.getFullName(),
                organization.getName(),
                event.getName(),
                host.getFullName(),
                host.getEmail(),
                request.getReason()
        );
        log.info("The event was cancelled by host, eventId={}", eventId);

    }

    @Override
    public void cancelEventByAdmin(UUID eventId, CancelEventRequest request) {
        //find the event
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check the event status cancelable?
        if (!EEventStatus.canEventBeCancelled(event.getStatus())) {
            throw new AppException(EventErrorCode.EVENT_CANNOT_CANCELLED);
        }

        //update event status to cancelled
        event.setStatus(EEventStatus.CANCELLED);
        eventRepository.save(event);

        //deduct the credit hour of the organization by 3
        Organization organization = event.getOrganization();
        organizationService.deductCreditHourOfOrganization(organization, 3);

        //cancel all applications of volunteer to the event
        List<EventApplication> eventApplications = eventApplicationService.cancelAllApplicationsOfEvent(event);

        //send notification to all the volunteer that applied to the event
        notificationService.sentEventCancelledByAdminNotification(eventApplications, event.getName(), request.getReason());

        //send email to the org manager
        OrganizationManager manager = organization.getOrganizationManager();
        emailService.sendEventCancelledByAdminEmail(
                manager.getEmail(),
                manager.getFullName(),
                organization.getName(),
                event.getName(),
                request.getReason()
        );
        log.info("The event was cancelled by admin, eventId={}", eventId);
    }

    @Override
    public UpdateEventResponse updateEvent(UUID eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check event status
        if (!EEventStatus.canEventBeUpdated(event.getStatus())) {
            throw new AppException(EventErrorCode.EVENT_CANNOT_UPDATED);
        }

        //handle update information, map request to payload
        boolean hasChanges = false;
        boolean updateCritical = false;
        UpdateEventPayload updatePayload = new UpdateEventPayload();
        UpdateEventResponse response = new UpdateEventResponse();

        //check update event sessions and other start date, end recruitment date and end date
        boolean updateEventDateTime = eventSessionService.checkAndResolveUpdateEventDateTime(event, request, updatePayload);
        if (updateEventDateTime) {
            hasChanges = true;
            updateCritical = true;
        }

        //map image
        if (request.getUpdateImages() != null && !request.getUpdateImages().isEmpty()) {
            hasChanges = true;
            response.setUploadUrls(eventImageService.resolveUpdateEventImagesPayload(event, request.getUpdateImages(), updatePayload));
        }
        if (request.getDescription() != null
                && !request.getDescription().isEmpty()
                && !request.getDescription().equalsIgnoreCase(event.getDescription()))
        {
            hasChanges = true;
            updatePayload.setDescription(request.getDescription());
        }

        if (request.getAutoApprove() != null
                && !request.getAutoApprove().equals(event.isAutoApprove())
        ) {
            hasChanges = true;
            updatePayload.setAutoApprove(request.getAutoApprove());
        }

        if (request.getServingPlaceType() != null
                && !request.getServingPlaceType().equals(event.getServingPlaceType())
        ) {
            hasChanges = true;
            updatePayload.setServingPlaceType(request.getServingPlaceType());
        }

        if (request.getAddress() != null
                && !request.getAddress().isEmpty()
                && !request.getAddress().equals(event.getAddress())
        ) {
            hasChanges = true;
            updateCritical = true;
            updatePayload.setAddress(request.getAddress());
        }

        if (request.getDetailAddress() != null
        && !request.getDetailAddress().isEmpty()
                && !request.getDetailAddress().equalsIgnoreCase(event.getDetailAddress())
        ) {
            hasChanges = true;
            updateCritical = true;
            updatePayload.setDetailAddress(request.getDetailAddress());
        }

        if (request.getCheckInLocationLat() != null
                && request.getCheckInLocationLng() != null
                && !request.getCheckInLocationLat().equals(GeoUtils.getLat(event.getCheckInLocation()))
                && !request.getCheckInLocationLng().equals(GeoUtils.getLng(event.getCheckInLocation()))
        )  {
            hasChanges = true;
            updateCritical = true;
            updatePayload.setCheckInLocationLat(request.getCheckInLocationLat());
            updatePayload.setCheckInLocationLng(request.getCheckInLocationLng());
        }

        if (request.getCheckInLocationAccuracyMeters() != null
            && (double) request.getCheckInLocationAccuracyMeters() != event.getCheckInAccuracyMeters()
        ) {
            hasChanges = true;
            updateCritical = true;
            updatePayload.setCheckInLocationAccuracyMeters((double)request.getCheckInLocationAccuracyMeters());
        }

        if (!hasChanges) {
            throw new AppException(EventErrorCode.NO_CHANGES_IN_UPDATE_REQUEST);
        }
        //set event's update information
        event.setUpdateEventPayload(updatePayload);
        event.setUpdateCritical(updateCritical);
        //set event  status to SUBMITTED
        event.setStatus(EEventStatus.SUBMITTED);
        eventRepository.save(event);

        //notify org manager about the update
        notificationService.sentEventUpdatedByHostNotification(
                event.getOrganization().getOrganizationManager().getId(),
                eventId,
                event.getName()
        );
        log.info("Event update request is submitted to Org Manager, eventId={}", eventId);

        return response;
    }

    @Override
    @Transactional
    public void assignHostToEvent(UUID eventId, AssignHostToEventRequest request) {
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );
        UUID oldHostId = event.getHost().getId();
        if (oldHostId.equals(request.getHostId())){
            throw new AppException(EventErrorCode.EVENT_CANNOT_ASSIGNED_TO_CURRENT_HOST);
        }
        //check event status
        if(!EEventStatus.canEventBeAssignedHost(event.getStatus())) {
            throw new AppException(EventErrorCode.EVENT_CANNOT_ASSIGNED_HOST);
        }

        //check account of host active
        if (!authService.checkAccountActive(request.getHostId())){
            throw new AppException(EventErrorCode.EVENT_CANNOT_ASSIGNED_TO_INACTIVE_HOST);
        }

        Host newHost = hostRepository.findById(request.getHostId()).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        //check host belong to the org
        if (newHost.getCreatedBy().getId() != currentUserProvider.getId()){
            throw new AppException(EventErrorCode.EVENT_CANNOT_ASSIGN_TO_HOST_NOT_IN_ORGANIZATION);
        }

        //check whether the host is hosting multiple event session in a day or not?
        List<EventSession> conflictSession =  eventSessionService.findConflictSessionDateOfHost(
                request.getHostId(),
                eventId,
                event.getSessions()
        );
        if (!conflictSession.isEmpty()) {
            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
        }

        //set host to this event
        event.setHost(newHost);
        eventRepository.save(event);

        //send notification to new and old host
        notificationService.sendEventAssignedHostNotification(oldHostId, newHost.getId(), event);

        log.info("Event assigned to host, eventId={} hostId={}", eventId, request.getHostId());
    }

    @Override
    @Transactional
    public void completeEvents() {
        //scan and get the event that 2 day passed from event endDate
        LocalDate targetDate = LocalDate.now().minusDays(2);
        List<Event> events = eventRepository.findEndedEventsAndEndDateBefore(targetDate);

        Set<Volunteer> updateVolunteerSet = new HashSet<>();
        Set<Volunteer> receiveCertVolunteerSet = new HashSet<>();
        List<VolunteerReview> newReviews = new ArrayList<>();
        for (Event event : events) {
            List<EventSession> sessions = event.getSessions();
            for (EventSession session : sessions) {
                    Duration duration = Duration.between(session.getStartDateTime(), session.getEndDateTime());
                    double requiredHours = duration.toMinutes() / 60.0 / 3;

                    //get applications that has checked in and checked out info
                    List<EligibleApplicationProjection> eligibleApplications =
                            eventApplicationRepository
                                    .findEligibleApplicationProjection(session.getId());

                    for (EligibleApplicationProjection projection : eligibleApplications) {
                        Volunteer volunteer = projection.getVolunteer();
                        EventApplication application = projection.getApplication();

                        //the application is legit but host has not reviewed it yet
                        if (projection.getReview() == null){
                            //auto review 5 stars for all legit participant
                            VolunteerReview review = volunteerReviewService
                                    .reviewAutomatically(volunteer, application);

                            //add the new review to the list for batch updating
                            newReviews.add(review);
                            projection.setReview(review);
                        }

                        int creditHour = application.getCreditHour();
                        double creditScore = (double) (projection.getReview().getAvgRating() * creditHour) /5;

                        //check the vol eligible to get cert
                        if (creditScore >= requiredHours) {
                            receiveCertVolunteerSet.add(volunteer);
                        }

                        //update the credit score of the volunteer
                        int creditToAdd = (int) Math.round(creditScore);
                        volunteer.setCreditScore(volunteer.getCreditScore() + creditToAdd);
                        updateVolunteerSet.add(volunteer);

                    }
                    //update batch in db of this session
                    //review for vol
                    volunteerReviewRepository.saveAll(newReviews);
                    log.info("Create automatic review for eligible volunteers of session, sessionId={}",session.getId());
                    newReviews.clear();
            }

            //update credit score for vol
            volunteerRepository.saveAll(updateVolunteerSet);
            log.info("Add credit score for volunteers of event, eventId={}",event.getId());
            updateVolunteerSet.clear();

            //generate certificates for volunteers
            certificateService.generateCertificates(receiveCertVolunteerSet.stream().toList(), event);
            log.info("Generate certificates for eligible volunteers of event, eventId={}",event.getId());
            //send notification to vol to inform about the certificates
            notificationService.sendVolunteersReceivedCertificatesNotifications(receiveCertVolunteerSet.stream().toList(), event);
            receiveCertVolunteerSet.clear();

            //update event status after process all the cert and vol score
            event.setStatus(EEventStatus.COMPLETED);
            eventRepository.save(event);
            log.info("Event completed, eventId={}", event.getId());

            //send notification to org mng and host
            notificationService.sendEventCompletedNotifications(
                    event,
                    event.getOrganization().getOrganizationManager().getId(),
                    event.getHost().getId()
            );
            log.info("Send event complete notification to org manager and host of event, eventId={}", event.getId());
        }
    }

    @Override
    @Transactional
    public void deleteEvent(UUID eventId) {
        //find the event
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_NOT_EXISTED)
        );

        //check the event status cancelable?
        if (!EEventStatus.canEventBeDeleted(event.getStatus())) {
            throw new AppException(EventErrorCode.EVENT_CANNOT_DELETED);
        }

        //delete the event from db
        eventRepository.delete(event);
        log.info("Event deleted, eventId={}", eventId);
    }

    @Override
    @Transactional
    public void endRecruitment() {
        //scan and get the event that passed from event recruitmentEndDate
        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<Event> events = eventRepository.findRecruitingEventsAndRecruitmentEndDateBefore(targetDate);

        for (Event event : events){
            //update event status to UPCOMING
            event.setStatus(EEventStatus.UPCOMING);
            eventRepository.save(event);
            log.info("Event status change to UPCOMING, eventId={}", event.getId());
        }

        //update event in db
        eventRepository.saveAll(events);
    }

    @Override
    @Transactional
    public void startEvents() {
        //scan and get the event that has the start date same as today
        LocalDate targetDate = LocalDate.now();
        List<Event> events = eventRepository.findUpcomingEventsAndStartDateToday(targetDate);

        for (Event event : events){
            //update event status to ONGOING
            event.setStatus(EEventStatus.ONGOING);
            eventRepository.save(event);
            log.info("Event status change to ONGOING, eventId={}", event.getId());
        }

        //update event in db
        eventRepository.saveAll(events);
    }

    @Override
    @Transactional
    public void endEvents() {
        //scan and get the event that has the end date is yesterday
        LocalDate targetDate = LocalDate.now().minusDays(1);
        List<Event> events = eventRepository.findOngoingEventsAndEndDateYesterday(targetDate);

        for (Event event : events){
            //update event status to ENDED
            event.setStatus(EEventStatus.ENDED);
            eventRepository.save(event);
            log.info("Event status change to ENDED, eventId={}", event.getId());
        }

        //update event in db
        eventRepository.saveAll(events);
    }

    @Override
    public Page<EventSimpleResponse> getSavedEventsByVolunteer(int pageNumber, int pageSize, String inputName) {
        UUID volunteerId = currentUserProvider.getId();

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Event> events = volunteerSavedEventRepository.findAllSavedEventsByVolunteerId(volunteerId, inputName, pageable);

        //check if there's no event with input status
        if(events.getContent().isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, events.getTotalElements());
        }

        //Map events to EventSimpleResponse
        return events.map(e -> {

            String firstEventImageUrl = null;

            //get signed URL of event images
            if (e.getImages() != null && !e.getImages().isEmpty()) {

                List<EventImage> eventImageList = e.getImages();

                CompletableFuture<String> firstEventImageFuture =
                        storageService.getSignedUrlAsync(eventImageList.getFirst().getImagePath());

                try {
                    CompletableFuture.allOf(firstEventImageFuture).join();
                    firstEventImageUrl = firstEventImageFuture.join();
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AppException ae) {
                        //todo: handle app exception in viewEventFeeds
                    } else {
                        throw cause instanceof RuntimeException re ? re : ex;
                    }
                }
            }

            return new EventSimpleResponse(
                    e.getId(),
                    e.getOrganization().getName(),
                    e.getName(),
                    firstEventImageUrl,
                    e.getAddress(),
                    e.getStartDate(),
                    e.getRecruitmentEndDate()
            );
        });
    }

    @Override
    public Page<EventSimpleResponse> getHostedEventsOfOrganization(int pageNumber, int pageSize, UUID organizationId, String eventName) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        List<String> approvedStatus = Stream.of(
                EEventStatus.COMPLETED
        ).map(Enum::name).toList();

        //Map events to EventSimpleResponse
        return eventRepository.findEventsByOrganizationIdAnd(
                organizationId,
                approvedStatus,
                eventName,
                pageable
        ).map(e -> {

            String firstEventImageUrl = null;

            //get signed URL of event images
            if (e.getImages() != null && !e.getImages().isEmpty()) {

                List<EventImage> eventImageList = e.getImages();

                CompletableFuture<String> firstEventImageFuture =
                        storageService.getSignedUrlAsync(eventImageList.getFirst().getImagePath());

                try {
                    CompletableFuture.allOf(firstEventImageFuture).join();
                    firstEventImageUrl = firstEventImageFuture.join();
                } catch (CompletionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof AppException ae) {
                        //todo: handle app exception in viewEventFeeds
                    } else {
                        throw cause instanceof RuntimeException re ? re : ex;
                    }
                }
            }

            return new EventSimpleResponse(
                    e.getId(),
                    e.getOrganization().getName(),
                    e.getName(),
                    firstEventImageUrl,
                    e.getAddress(),
                    e.getStartDate(),
                    e.getRecruitmentEndDate()
            );
        });
    }
}

