package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventImagePayload;
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
import java.util.function.Function;
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

    //todo unit test for this method
    /**
     * @return list of upload image url
     */
    @Override
    public List<String> resolveUpdateEventImagesPayload(
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

        List<String> uploadUrls = new ArrayList<>();
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

            uploadUrls =  urlFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

        }

        //set images' info after update to payload
        List<UpdateEventImagePayload> imagePayloads = eventImagesAfterUpdate.stream().map(img -> {
            UpdateEventImagePayload payload = new UpdateEventImagePayload();
            payload.setId(img.getId());
            payload.setImagePath(img.getImagePath());
            return payload;
        }).toList();
        updateEventPayload.setEventImages(imagePayloads);

        return uploadUrls;
    }

    //todo unit test for this method
    @Override
    public List<EventImage> resolveUpdatedEventImages(
            Event event,
            List<EventImage> oldImages,
            List<UpdateEventImagePayload> payloads
    ) {
        // Map existing
        Map<UUID, EventImage> existing = oldImages.stream()
                .filter(img -> img.getId() != null)
                .collect(Collectors.toMap(EventImage::getId, Function.identity()));

        List<EventImage> result = new ArrayList<>(payloads.size());
        Set<UUID> keepIds = new HashSet<>();

        // Build new list + mark keep
        for (UpdateEventImagePayload p : payloads) {
            UUID id = p.getId();

            if (id != null && existing.containsKey(id)) {
                EventImage e = existing.get(id);
                e.setImagePath(p.getImagePath());
                result.add(e);
                keepIds.add(id);
            } else {
                EventImage e = new EventImage();
                e.setId(id);
                e.setImagePath(p.getImagePath());
                e.setEvent(event);
                result.add(e);
            }
        }

        // Delete removed
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (EventImage old : oldImages) {
            UUID id = old.getId();
            if (id != null && !keepIds.contains(id)) {
                futures.add(storageService.deleteFileAsync(old.getImagePath()));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        oldImages.clear();
        oldImages.addAll(result);

       return oldImages;
    }
}
