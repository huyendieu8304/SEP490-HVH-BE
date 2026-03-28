package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EEventStatus;
import com.sep490.g28.hvh.be.dto.event.request.CancelEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.EditEventRequest;
import com.sep490.g28.hvh.be.dto.event.request.RejectEventRequest;
import com.sep490.g28.hvh.be.dto.event.response.*;
import com.sep490.g28.hvh.be.dto.event.request.SaveEventRequest;
import com.sep490.g28.hvh.be.dto.notification.request.AnnounceVolunteerRequest;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.ActivityDomainErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
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

    CurrentUserProvider currentUserProvider;

    EventMapper eventMapper;

    @Override
    public EventFeedResponse getEventFeeds(int pageNumber, int pageSize, boolean refresh,
                                           String name, String address, LocalDate startDate,
                                           LocalDate endDate, List<Short> activitySubDomains) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        //Get the slice based on the current action is refresh (swipe up) or load more (scroll end)
        Page<Event> page = null;


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

        ActivityDomain activityDomain = activitySubDomain.getActivityDomain();
        Short sessionMaxTime = activityDomain.getSpecialSessionMaxTime() == null ? 4 : activityDomain.getSpecialSessionMaxTime();
        eventSessionService.addEventSessionsForCreateEvent(
                event,
                request.getRecruitmentEndDate(),
                request.getEventSessions(),
                sessionMaxTime
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

        ActivityDomain activityDomain = activitySubDomain.getActivityDomain();
        Short sessionMaxTime =
                activityDomain.getSpecialSessionMaxTime() == null
                        ? 4
                        : activityDomain.getSpecialSessionMaxTime();
        eventSessionService.updateEventSessions(
                event,
                request.getRecruitmentEndDate(),
                request.getEventSessions(),
                sessionMaxTime
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
        Host host = hostRepository.getReferenceById(currentUserProvider.getId());
        Organization organization = host.getOrganization();

        event.setHost(host);
        event.setCreateBy(host);
        event.setOrganization(organization);

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

        event.setRecruitmentEndDate(request.getRecruitmentEndDate());
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
                        es.getExpectedSerAmount()
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

        //check whether the host is hosting other event or not?
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
        log.info("Event is approved by Organization Manager: eventId={}", event.getId());

        //send notification
        notificationService.sendEventApprovedByOrgManagerNotification(event);
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

        //update in db
        event.setStatus(EEventStatus.REJECTED_BY_MNG);
        eventRepository.save(event);
        log.info("Event is rejected by Organization Manager: eventId={}", event.getId());

        //send notification
        notificationService.sendEventRejectedByOrgManagerNotification(event, request.getReason());
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

//        //check whether the host is hosting other event or not?
//        List<EventSession> conflictSession =
//                eventSessionService.findConflictSessionDateOfHost(
//                        event.getHost().getId(),
//                        eventId,
//                        event.getDateTimes()
//                );
//        if (!conflictSession.isEmpty()) {
//            throw new AppException(EventErrorCode.DUPLICATE_HOSTED_DATE);
//        }

        //update in db
        event.setStatus(EEventStatus.RECRUITING);
        eventRepository.save(event);
        log.info("Event is approved by System Admin: eventId={}", event.getId());

        //send notification
        notificationService.sendEventApprovedByAdminNotification(event);
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

        //update in db
        event.setStatus(EEventStatus.REJECTED_BY_AD);
        eventRepository.save(event);
        log.info("Event is rejected by System Admin: eventId={}", event.getId());

        //send notification
        notificationService.sendEventRejectedByAdminNotification(event, request.getReason());
    }

    //todo unit test for this method
    @Override
    public Page<EventSimpleResponseForManager> getPendingEventsByManager(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.ASC, "created_at")
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
                Sort.by(Sort.Direction.ASC, "created_at")
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
                Sort.by(Sort.Direction.ASC, "created_at")
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
                Sort.by(Sort.Direction.ASC, "created_at")
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

        //todo check if event belongs to org

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
                        es.getExpectedSerAmount()
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
                                es.getExpectedSerAmount()
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
                        es.getExpectedSerAmount()
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
                                es.getExpectedSerAmount()
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
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        EEventStatus status =
                (inputStatus == null || inputStatus.isBlank())
                        ? null
                        : EEventStatus.valueOf(inputStatus);

        Page<Event> events = eventRepository.findEventsByHostId(hostId, status, eventName, pageable);

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
                        es.getExpectedSerAmount()
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
            throw new AppException(EventErrorCode.EVENT_CANNOT_CANCEL);
        }

        //update event status to cancelled
        event.setStatus(EEventStatus.CANCELLED);
        eventRepository.save(event);

        //deduct the credit hour of the organization by 3
        Organization organization = event.getOrganization();
        organizationService.deductCreditHourOfOrganization(organization, 3);

        //cancel all applications of volunteer to the event
        List<EventApplication> eventApplications = eventApplicationService.cancelAllApplicationsToEvent(event);

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
            throw new AppException(EventErrorCode.EVENT_CANNOT_CANCEL);
        }

        //update event status to cancelled
        event.setStatus(EEventStatus.CANCELLED);
        eventRepository.save(event);

        //deduct the credit hour of the organization by 3
        Organization organization = event.getOrganization();
        organizationService.deductCreditHourOfOrganization(organization, 3);

        //cancel all applications of volunteer to the event
        List<EventApplication> eventApplications = eventApplicationService.cancelAllApplicationsToEvent(event);

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


}

