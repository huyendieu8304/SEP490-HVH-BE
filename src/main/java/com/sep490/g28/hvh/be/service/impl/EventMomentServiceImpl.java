package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEventApplicationStatus;
import com.sep490.g28.hvh.be.dto.eventapplication.response.EventApplicationsResponse;
import com.sep490.g28.hvh.be.dto.eventapplication.response.RegisteredParticipantSimpleResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.request.ShareMomentRequest;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedDetailsResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentFeedResponse;
import com.sep490.g28.hvh.be.dto.eventmoment.response.EventMomentResponseForVolunteer;
import com.sep490.g28.hvh.be.dto.eventmoment.response.ShareMomentResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import com.sep490.g28.hvh.be.repository.EventMomentRepository;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import com.sep490.g28.hvh.be.service.EventMomentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventMomentServiceImpl implements EventMomentService {
    EventApplicationRepository eventApplicationRepository;
    EventSessionRepository eventSessionRepository;
    EventMomentRepository eventMomentRepository;

    StorageService storageService;
    StoragePathGenerator storagePathGenerator;
    CurrentUserProvider currentUserProvider;

    @Override
    public ShareMomentResponse shareMoment(ShareMomentRequest request) {
        OffsetDateTime shareMomentTime = OffsetDateTime.now();

        UUID volunteerId = currentUserProvider.getId();

        EventApplication eventApplication = eventApplicationRepository
                .findByVolunteerIdAndSessionId(volunteerId, UUID.fromString(request.getEventSessionId()));

        //check if event application exists
        if (eventApplication == null) {
            throw new AppException(EventErrorCode.EVENT_APPLICATION_NOT_EXISTED);
        }

        //find today's vol event
        EventSession eventSession = eventSessionRepository.findById(UUID.fromString(request.getEventSessionId())).orElseThrow(
                () -> new AppException(EventErrorCode.EVENT_SESSION_NOT_EXISTED)
        );

        //Check if event session is started
        if (!shareMomentTime.isAfter(eventSession.getStartDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_NOT_STARTED);
        }

        //Check if event session is ended
        if (!shareMomentTime.isBefore(eventSession.getEndDateTime())) {
            throw new AppException(EventErrorCode.EVENT_SESSION_ENDED);
        }

        //check if any event moment is shared in this session
        EventMoment eventMoment = eventMomentRepository.findByEventApplicationId(eventApplication.getId());
        if (eventMoment != null) {
            throw new AppException(EventErrorCode.EVENT_MOMENT_ALREADY_SHARED);
        }

        //get the moment's picture path in storage
        List<String> momentPicturesPathsList = new ArrayList<>();
        if (request.getMomentPictures() != null) {
            String[] momentPictures = request.getMomentPictures().split("\\s+");
            int legal_order = 1;
            for (String momentPicture : momentPictures) {
                String momentPicturePath = storagePathGenerator
                        .eventMomentImages(eventSession.getEvent().getId(), eventApplication.getId(), legal_order++, momentPicture);
                momentPicturesPathsList.add(momentPicturePath);
                if (legal_order == 6) {
                    break;
                }
            }
        }

        StringBuilder momentPicturesPathsSB = new StringBuilder();
        for (String momentPicturePath : momentPicturesPathsList) {
            momentPicturesPathsSB.append(momentPicturePath).append(" ");
        }
        String momentPicturesPaths = momentPicturesPathsSB.toString().trim();

        //get upload urls for moment's pictures
        List<CompletableFuture<String>> momentPicturesFutures = new ArrayList<>();
        for (String momentPicturePath : momentPicturesPathsList) {
            CompletableFuture<String> momentPictureFuture =
                    storageService.getUploadUrlAsync(momentPicturePath);
            momentPicturesFutures.add(momentPictureFuture);
        }

        try {
            CompletableFuture.allOf(momentPicturesFutures.toArray(new CompletableFuture[0])).join();

        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }

        List<String> momentPicturesUploadUrl = new ArrayList<>();
        for (CompletableFuture<String> momentPictureFuture : momentPicturesFutures) {
            momentPicturesUploadUrl.add(momentPictureFuture.join());
        }

        //save event moment into db
        EventMoment newEventMoment = new EventMoment();
        newEventMoment.setEventApplication(eventApplication);
        newEventMoment.setMomentContent(request.getMomentContent());
        newEventMoment.setMomentPictures(momentPicturesPaths);
        eventMomentRepository.save(newEventMoment);

        return ShareMomentResponse.builder()
                .momentPicturesUploadUrls(momentPicturesUploadUrl)
                .build();
    }

    @Override
    public EventMomentFeedResponse getEventMomentsFeed(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventMoment> eventMoments = eventMomentRepository.findAllByName(eventName, pageable);

        //map event moment to response
        List<EventMomentFeedDetailsResponse> responses = Optional.of(eventMoments.getContent())
                .map(list -> list.stream()
                        .map(e -> {

                            UUID volunteerId = null;
                            String nickName = null;
                            String name = null;
                            String avatarUrl = null;
                            CompletableFuture<String> avatarFuture = null;
                            List<String> momentPicturesUrls = new ArrayList<>();

                            EventApplication eventApplication = e.getEventApplication();
                            Event event = eventApplication.getSession().getEvent();

                            //check if the event application linked with a volunteer
                            if (eventApplication.getVolunteer() != null) {

                                Volunteer volunteer = eventApplication.getVolunteer();

                                volunteerId = volunteer.getId();
                                nickName = volunteer.getNickname();
                                name = volunteer.getFullName();


                                //check if volunteer has avatar
                                if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {
                                    avatarFuture = storageService.getSignedUrlAsync(volunteer.getAvatarUrl());
                                }
                            }

                            //get signed URL of moment pictures and volunteer avatar (if exist)
                            List<CompletableFuture<String>> momentPicturesFutures = new ArrayList<>();
                            if (e.getMomentPictures() != null) {
                                String[] momentPictures = e.getMomentPictures().split("\\s+");
                                List<String> momentPicturesList = new ArrayList<>(Arrays.asList(momentPictures));
                                for (String momentPicture : momentPicturesList) {
                                    CompletableFuture<String> momentPictureFuture =
                                            storageService.getSignedUrlAsync(momentPicture);
                                    momentPicturesFutures.add(momentPictureFuture);
                                }
                            }

                            try {

                                if (avatarFuture != null) {
                                    CompletableFuture.allOf(avatarFuture).join();
                                    avatarUrl = avatarFuture.join();
                                }
                                CompletableFuture.allOf(momentPicturesFutures.toArray(new CompletableFuture[0])).join();


                                for (CompletableFuture<String> momentPictureFuture : momentPicturesFutures) {
                                    momentPicturesUrls.add(momentPictureFuture.join());
                                }

                            } catch (CompletionException ex) {
                                Throwable cause = ex.getCause();
                                if (cause instanceof AppException ae) {
                                    //todo handle here
                                } else {
                                    throw cause instanceof RuntimeException re ? re : ex;
                                }
                            }

                            return new EventMomentFeedDetailsResponse(
                                    volunteerId,
                                    nickName,
                                    name,
                                    avatarUrl,

                                    event.getId(),
                                    event.getName(),
                                    event.getAddress(),
                                    event.getDetailAddress(),

                                    e.getId(),
                                    e.getMomentContent(),
                                    momentPicturesUrls,
                                    e.getCreatedAt()
                            );
                        }).toList()).orElse(Collections.emptyList());

        // If after load the page with n size,
        // and page.hasNext() is true (the slice will auto check this)
        // , move the cursor to the next page, which will load more content
        // (equivalent to call the api one more time)
        return new EventMomentFeedResponse(
                responses,
                eventMoments.hasNext() ? String.valueOf(pageNumber + 1) : null,
                eventMoments.hasNext()
        );
    }

    @Override
    public void deleteEventMoment(UUID eventMomentId) {
        EventMoment eventMoment = eventMomentRepository.findById(eventMomentId)
                .orElseThrow(() -> new AppException(EventErrorCode.EVENT_MOMENT_NOT_FOUND));
        eventMomentRepository.delete(eventMoment);
    }

    @Override
    public Page<EventMomentFeedDetailsResponse> getEventMomentsForVolunteer(int pageNumber, int pageSize, String eventName) {

        UUID volunteerId = currentUserProvider.getId();

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventMoment> eventMoments = eventMomentRepository.findAllByVolunteerId(volunteerId, eventName, pageable);

        return eventMoments.map(e -> {
            String nickName = null;
            String name = null;
            String avatarUrl = null;
            CompletableFuture<String> avatarFuture = null;
            List<String> momentPicturesUrls = new ArrayList<>();

            EventApplication eventApplication = e.getEventApplication();
            Event event = eventApplication.getSession().getEvent();

            //check if the event application linked with a volunteer
            if (eventApplication.getVolunteer() != null) {

                Volunteer volunteer = eventApplication.getVolunteer();

                nickName = volunteer.getNickname();
                name = volunteer.getFullName();


                //check if volunteer has avatar
                if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {
                    avatarFuture = storageService.getSignedUrlAsync(volunteer.getAvatarUrl());
                }
            }

            //get signed URL of moment pictures and volunteer avatar (if exist)
            List<CompletableFuture<String>> momentPicturesFutures = new ArrayList<>();
            if (e.getMomentPictures() != null) {
                String[] momentPictures = e.getMomentPictures().split("\\s+");
                List<String> momentPicturesList = new ArrayList<>(Arrays.asList(momentPictures));
                for (String momentPicture : momentPicturesList) {
                    CompletableFuture<String> momentPictureFuture =
                            storageService.getSignedUrlAsync(momentPicture);
                    momentPicturesFutures.add(momentPictureFuture);
                }
            }

            try {

                if (avatarFuture != null) {
                    CompletableFuture.allOf(avatarFuture).join();
                    avatarUrl = avatarFuture.join();
                }
                CompletableFuture.allOf(momentPicturesFutures.toArray(new CompletableFuture[0])).join();


                for (CompletableFuture<String> momentPictureFuture : momentPicturesFutures) {
                    momentPicturesUrls.add(momentPictureFuture.join());
                }

            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof AppException ae) {
                    //todo handle here
                } else {
                    throw cause instanceof RuntimeException re ? re : ex;
                }
            }

            return new EventMomentFeedDetailsResponse(
                    volunteerId,
                    nickName,
                    name,
                    avatarUrl,

                    event.getId(),
                    event.getName(),
                    event.getAddress(),
                    event.getDetailAddress(),

                    e.getId(),
                    e.getMomentContent(),
                    momentPicturesUrls,
                    e.getCreatedAt()
            );
        });
    }

    @Override
    public Page<EventMomentFeedDetailsResponse> getEventMomentsOfEvent(int pageNumber, int pageSize, UUID eventId, String eventName) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<EventMoment> eventMoments = eventMomentRepository.findAllByEventId(eventId, eventName, pageable);

        return eventMoments.map(e -> {
            UUID volunteerId = null;
            String nickName = null;
            String name = null;
            String avatarUrl = null;
            CompletableFuture<String> avatarFuture = null;
            List<String> momentPicturesUrls = new ArrayList<>();

            EventApplication eventApplication = e.getEventApplication();
            Event event = eventApplication.getSession().getEvent();

            Volunteer volunteer = eventApplication.getVolunteer();

            //check if the event application linked with a volunteer
            if (volunteer != null) {

                volunteerId = volunteer.getId();
                nickName = volunteer.getNickname();
                name = volunteer.getFullName();


                //check if volunteer has avatar
                if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {
                    avatarFuture = storageService.getSignedUrlAsync(volunteer.getAvatarUrl());
                }
            }

            //get signed URL of moment pictures and volunteer avatar (if exist)
            List<CompletableFuture<String>> momentPicturesFutures = new ArrayList<>();
            if (e.getMomentPictures() != null) {
                String[] momentPictures = e.getMomentPictures().split("\\s+");
                List<String> momentPicturesList = new ArrayList<>(Arrays.asList(momentPictures));
                for (String momentPicture : momentPicturesList) {
                    CompletableFuture<String> momentPictureFuture =
                            storageService.getSignedUrlAsync(momentPicture);
                    momentPicturesFutures.add(momentPictureFuture);
                }
            }

            try {

                if (avatarFuture != null) {
                    CompletableFuture.allOf(avatarFuture).join();
                    avatarUrl = avatarFuture.join();
                }
                CompletableFuture.allOf(momentPicturesFutures.toArray(new CompletableFuture[0])).join();


                for (CompletableFuture<String> momentPictureFuture : momentPicturesFutures) {
                    momentPicturesUrls.add(momentPictureFuture.join());
                }

            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof AppException ae) {
                    //todo handle here
                } else {
                    throw cause instanceof RuntimeException re ? re : ex;
                }
            }


            return new EventMomentFeedDetailsResponse(
                    volunteerId,
                    nickName,
                    name,
                    avatarUrl,

                    event.getId(),
                    event.getName(),
                    event.getAddress(),
                    event.getDetailAddress(),

                    e.getId(),
                    e.getMomentContent(),
                    momentPicturesUrls,
                    e.getCreatedAt()
            );
        });
    }
}
