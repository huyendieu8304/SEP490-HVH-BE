package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventImage;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.EventImageRepository;
import com.sep490.g28.hvh.be.service.EventImageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EventImageServiceImpl implements EventImageService {

    private final EventImageRepository eventImageRepository;

    private final StoragePathGenerator storagePathGenerator;

    private final StorageService storageService;

    private static final int MAX_IMAGES = 5;


    public List<String> addEventImages(Event event, List<EditEventImageRequest> addImages) {
        if (addImages == null || addImages.isEmpty()) return Collections.emptyList();

        //check request: valid add image amount?
        int countAddImages = (int) addImages.stream()
                .filter(r -> r.getUpdateAction() == EUpdateAction.ADD)
                .count();
        if (countAddImages > MAX_IMAGES) {
            throw new AppException(EventErrorCode.INVALID_IMAGES_AMOUNT);
        }
        return addEventImages(event, addImages, MAX_IMAGES);
    }

    private List<String> addEventImages(Event event, List<EditEventImageRequest> addImages, int remainingSlot) {
        List<CompletableFuture<String>> urlFutures = new ArrayList<>();
        //go through add list to create object EventImage and get upload url
        addImages.stream()
                .filter(r -> r.getUpdateAction() == EUpdateAction.ADD)
                .limit(remainingSlot)
                .forEach(r -> {

                    UUID imageId = UUID.randomUUID();
                    String path = storagePathGenerator.eventImage(
                            event.getId(),
                            imageId,
                            r.getFileExtension()
                    );

                    EventImage image = new EventImage();
                    image.setId(imageId);
                    image.setEvent(event);
                    image.setImagePath(path);

                    //add to Event
                    event.getImages().add(image);

                    urlFutures.add(storageService.getUploadUrlAsync(path));
                });

        CompletableFuture.allOf(urlFutures.toArray(new CompletableFuture[0])).join();

        return urlFutures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    public List<String> updateEventImages(Event event, List<EditEventImageRequest> reqImages) {
        //request image empty, don't need to update
        if (reqImages == null || reqImages.isEmpty()) return Collections.emptyList();

        List<EventImage> existingImages = event.getImages();

        //categorize update image request base on action
        List<EditEventImageRequest> removes = new ArrayList<>();
        List<EditEventImageRequest> adds = new ArrayList<>();

        for (EditEventImageRequest r : reqImages) {
            if (r.getUpdateAction().equals(EUpdateAction.REMOVE)) {
                removes.add(r);
            } else
                adds.add(r);
        }

        //check the amount
        Set<UUID> existingIds = existingImages.stream()
                .map(EventImage::getId)
                .collect(Collectors.toSet());

        Set<UUID> removeIds = removes.stream()
                .map(EditEventImageRequest::getImageId)
                .filter(Objects::nonNull)
                .filter(existingIds::contains)
                .collect(Collectors.toSet());

        int existingCount = existingImages.size();
        int removeCount =  removeIds.size();
        int addCount = adds.size();

        //the amount of event's images after update
        int finalCount = existingCount - removeCount + addCount;
        if (finalCount > MAX_IMAGES) {
            throw new AppException(EventErrorCode.INVALID_IMAGES_AMOUNT);
        }

        //remove
        if (!removeIds.isEmpty()) {

            existingImages.removeIf(dt -> removeIds.contains(dt.getId()));

            // get path to delete
            List<String> pathsToDelete = existingImages.stream()
                    .filter(img -> removeIds.contains(img.getId()))
                    .map(EventImage::getImagePath)
                    .toList();

            // delete file async
            List<CompletableFuture<Void>> futures = pathsToDelete.stream()
                    .map(storageService::deleteFileAsync)
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }

        //add
        if (!adds.isEmpty()) {
            int remainingSlots = MAX_IMAGES - (existingCount - removeCount);
            return addEventImages(event, adds, remainingSlots);
        }

        return Collections.emptyList();
    }

    /**
     * @return list of upload image url
     */
    @Override
    public List<String> resolveUpdateEventImages(
            Event event,
            List<EditEventImageRequest> reqImages,
            UpdateEventPayload updateEventPayload
    ) {

        //clone the existing image to new list
        List<EventImage> eventImagesAfterUpdate = new ArrayList<>(event.getImages().stream()
                .map(EventImage::new)
                .toList());

        //categorize update image request base on action
        List<EditEventImageRequest> removes = new ArrayList<>();
        List<EditEventImageRequest> adds = new ArrayList<>();

        for (EditEventImageRequest r : reqImages) {
            if (r.getUpdateAction().equals(EUpdateAction.REMOVE)) {
                removes.add(r);
            } else
                adds.add(r);
        }

        //check the amount
        Set<UUID> existingIds = eventImagesAfterUpdate.stream()
                .map(EventImage::getId)
                .collect(Collectors.toSet());

        Set<UUID> removeIds = removes.stream()
                .map(EditEventImageRequest::getImageId)
                .filter(Objects::nonNull)
                .filter(existingIds::contains)
                .collect(Collectors.toSet());

        int existingCount = eventImagesAfterUpdate.size();
        int removeCount =  removeIds.size();
        int addCount = adds.size();

        //the amount of event's images after update
        int finalCount = existingCount - removeCount + addCount;
        if (finalCount > MAX_IMAGES) {
            throw new AppException(EventErrorCode.INVALID_IMAGES_AMOUNT);
        }

        //remove
        if (!removeIds.isEmpty()) {
            eventImagesAfterUpdate.removeIf(dt -> removeIds.contains(dt.getId()));
        }

        //add
        if (!adds.isEmpty()) {
            int remainingSlot = MAX_IMAGES - (existingCount - removeCount);
            List<CompletableFuture<String>> urlFutures = new ArrayList<>();
            //go through add list to create object EventImage and get upload url
            adds.stream()
                    .filter(r -> r.getUpdateAction() == EUpdateAction.ADD)
                    .limit(remainingSlot)
                    .forEach(r -> {
                        //create object EventImage
                        UUID imageId = UUID.randomUUID();
                        String path = storagePathGenerator.eventImage(
                                event.getId(),
                                imageId,
                                r.getFileExtension()
                        );

                        EventImage image = new EventImage();
                        image.setId(imageId);
                        image.setEvent(event);
                        image.setImagePath(path);

                        //add to the list
                        eventImagesAfterUpdate.add(image);

                        urlFutures.add(storageService.getUploadUrlAsync(path));
                    });

            CompletableFuture.allOf(urlFutures.toArray(new CompletableFuture[0])).join();

            return urlFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

        }
        updateEventPayload.setEventImages(eventImagesAfterUpdate);
        return Collections.emptyList();
    }

    public void deleteRemovedImage(List<EventImage> oldImages, List<EventImage> newImages) {
        //get ids of images that would be kept or added
        Set<UUID> newIds = newImages.stream()
                .map(EventImage::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        //get the ids of removed images
        List<EventImage> removedImages = oldImages.stream()
                .filter(oldImg -> oldImg.getId() != null)
                .filter(oldImg -> !newIds.contains(oldImg.getId()))
                .toList();

        // get path to delete
        List<String> pathsToDelete = removedImages.stream()
                .map(EventImage::getImagePath)
                .toList();

        // delete file async
        List<CompletableFuture<Void>> futures = pathsToDelete.stream()
                .map(storageService::deleteFileAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        log.info("Deleted path(s) of removed images");
    }
}
