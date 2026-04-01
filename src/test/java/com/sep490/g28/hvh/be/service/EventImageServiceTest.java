package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.constant.EUpdateAction;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventImage;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.service.impl.EventImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventImageServiceTest {

//    @Mock
//    EventImageRepository eventImageRepository;

    @Mock
    StoragePathGenerator storagePathGenerator;

    @Mock
    StorageService storageService;

    @InjectMocks
    EventImageServiceImpl service;

    Event event;

    private static final int MAX_IMAGES = 5; // phải match constant thật


    @BeforeEach
    void setup() {
        event = new Event();
        event.setId(UUID.randomUUID());
        event.setImages(new ArrayList<>());
    }

    private Event eventWithImages(int n) {
        Event e = new Event();
        e.setId(UUID.randomUUID());

        List<EventImage> imgs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            EventImage img = new EventImage();
            img.setId(UUID.randomUUID());
            img.setEvent(e);
            img.setImagePath("old-" + i);
            imgs.add(img);
        }
        e.setImages(imgs);
        return e;
    }

    private EditEventImageRequest addReq() {
        EditEventImageRequest r = new EditEventImageRequest();
        r.setUpdateAction(EUpdateAction.ADD);
        r.setFileExtension("jpg");
        return r;
    }

    private EditEventImageRequest removeReq(UUID id) {
        EditEventImageRequest r = new EditEventImageRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        r.setImageId(id);
        return r;
    }

    private EditEventImageRequest removeReq() {
        EditEventImageRequest r = new EditEventImageRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        return r;
    }

    private EventImage image(UUID id) {
        EventImage img = new EventImage();
        img.setId(id);
        img.setEvent(event);
        img.setImagePath("event/" + UUID.randomUUID() + "/image/" + id +".jpg");
        return img;
    }

    // ==== addEventImages (3 params) ===================================
    // TC01 addImages == null
    @Test
    void addEventImages_nullRequest_shouldReturnEmptyList() {
        assertTrue(service.addEventImages(event, null).isEmpty());
    }

    // TC02 add 1 image
    @Test
    void addEventImages_oneAdd_shouldReturnUrl() {

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path/img.jpg");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> result = service.addEventImages(event, List.of(addReq()));

        assertEquals(1, result.size());
        assertEquals(1, event.getImages().size());
    }

    // TC03 add exactly MAX_IMAGES
    @Test
    void addEventImages_exactMax_shouldSuccess() {

        List<EditEventImageRequest> req = List.of(
                addReq(), addReq(), addReq(), addReq(), addReq()
        );

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path/img.jpg");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> result = service.addEventImages(event, req);

        assertEquals(5, result.size());
        assertEquals(5, event.getImages().size());
    }

    // TC04 add > MAX_IMAGES
    @Test
    void addEventImages_exceedMax_shouldThrow() {

        List<EditEventImageRequest> req = Arrays.asList(
                addReq(), addReq(), addReq(),
                addReq(), addReq(), addReq()
        );

        assertThrows(AppException.class,
                () -> service.addEventImages(event, req));
    }

    // TC05 mix ADD + REMOVE
    @Test
    void addEventImages_mixedActions_shouldProcessOnlyAdd() {

        List<EditEventImageRequest> req = List.of(
                addReq(),
                removeReq(),
                addReq()
        );

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path/img.jpg");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> result = service.addEventImages(event, req);

        assertEquals(2, result.size());
        assertEquals(2, event.getImages().size());
    }

    // TC06 empty request
    @Test
    void addEventImages_emptyRequest_shouldReturnEmptyList() {
        assertTrue(service.addEventImages(event,  List.of()).isEmpty());
    }


    // ==== updateEventImages ===================================
    // TC01 reqImages null
    @Test
    void updateEventImages_nullRequest_shouldReturnEmpty() {

        List<String> result = service.updateEventImages(event, null);

        assertTrue(result.isEmpty());
    }

    // TC02 empty request
    @Test
    void updateEventImages_emptyRequest_shouldReturnEmpty() {

        List<String> result = service.updateEventImages(event, List.of());

        assertTrue(result.isEmpty());
    }

    // TC03 remove existing
    @Test
    void updateEventImages_removeExisting_shouldRemoveFromEvent() {

        UUID id = UUID.randomUUID();

        EventImage img = image(id);
        event.getImages().add(img);

        service.updateEventImages(event, List.of(removeReq(id)));

        assertTrue(event.getImages().isEmpty());
    }

    // TC04 remove id not exist
    @Test
    void updateEventImages_removeNotExisting_shouldIgnore() {

        service.updateEventImages(event, List.of(removeReq(UUID.randomUUID())));

        assertTrue(event.getImages().isEmpty());
    }

    // TC05 add image
    @Test
    void updateEventImages_add_shouldReturnUrl() {

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path/img.jpg");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> result = service.updateEventImages(event, List.of(addReq()));

        assertEquals(1, result.size());
        assertEquals(1, event.getImages().size());
    }

    // TC06 remove then add
    @Test
    void updateEventImages_removeThenAdd_shouldWork() {

        UUID id = UUID.randomUUID();
        event.getImages().add(image(id));

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path/img.jpg");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<EditEventImageRequest> req = List.of(
                removeReq(id),
                addReq()
        );

        List<String> result = service.updateEventImages(event, req);

        assertEquals(1, result.size());
    }

    // TC07 exceed MAX
    @Test
    void updateEventImages_exceedMax_shouldThrow() {

        for (int i = 0; i < 5; i++) {
            event.getImages().add(image(UUID.randomUUID()));
        }

        assertThrows(AppException.class,
                () -> service.updateEventImages(event, List.of(addReq())));
    }

    // TC08 remove id null
    @Test
    void updateEventImages_removeNullId_shouldIgnore() {

        EditEventImageRequest r = new EditEventImageRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);

        List<String> result = service.updateEventImages(event, List.of(r));

        assertTrue(result.isEmpty());
    }

    // ===== resolveUpdateEventImagesPayload
    // TC1: only remove
    @Test
    void resolveUpdateEventImagesPayload_onlyRemove_shouldWork() {
        Event e = eventWithImages(2);
        UpdateEventPayload payload = new UpdateEventPayload();

        UUID removeId = e.getImages().get(0).getId();

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                List.of(removeReq(removeId)),
                payload
        );

        assertTrue(res.isEmpty());
        assertEquals(1, payload.getEventImages().size());
    }

    // TC2: only add
    @Test
    void resolveUpdateEventImagesPayload_onlyAdd_shouldReturnUploadUrls() {
        Event e = eventWithImages(1);
        UpdateEventPayload payload = new UpdateEventPayload();

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                List.of(addReq(), addReq()),
                payload
        );

        assertEquals(2, res.size());
        assertEquals(3, payload.getEventImages().size());
    }

    // TC3: remove + add
    @Test
    void resolveUpdateEventImagesPayload_removeAndAdd_shouldWork() {
        Event e = eventWithImages(2);
        UpdateEventPayload payload = new UpdateEventPayload();

        UUID removeId = e.getImages().get(0).getId();

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                List.of(removeReq(removeId), addReq()),
                payload
        );

        assertEquals(1, res.size());
        assertEquals(2, payload.getEventImages().size());
    }

    // TC4: exceed MAX_IMAGES
    @Test
    void resolveUpdateEventImagesPayload_exceedMaxImages_shouldThrow() {
        Event e = eventWithImages(MAX_IMAGES);
        UpdateEventPayload payload = new UpdateEventPayload();

        assertThrows(AppException.class,
                () -> service.resolveUpdateEventImagesPayload(
                        e,
                        List.of(addReq()),
                        payload
                ));
    }

    // TC5: removeIds empty (id null or not exist)
    @Test
    void resolveUpdateEventImagesPayload_removeIdsEmpty_shouldIgnore() {
        Event e = eventWithImages(2);
        UpdateEventPayload payload = new UpdateEventPayload();

        EditEventImageRequest r = new EditEventImageRequest();
        r.setUpdateAction(EUpdateAction.REMOVE);
        r.setImageId(UUID.randomUUID()); // not exist

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                List.of(r),
                payload
        );

        assertEquals(2, payload.getEventImages().size());
    }

    // TC6: adds empty
    @Test
    void resolveUpdateEventImagesPayload_addsEmpty_shouldNoUpload() {
        Event e = eventWithImages(2);
        UpdateEventPayload payload = new UpdateEventPayload();

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                Collections.emptyList(),
                payload
        );

        assertTrue(res.isEmpty());
        assertEquals(2, payload.getEventImages().size());
    }

    // TC7: limit by remainingSlot
    @Test
    void resolveUpdateEventImages_Payload_addExceedRemainingSlot_shouldLimit() {
        Event e = eventWithImages(MAX_IMAGES - 3); //exist 2 image
        UpdateEventPayload payload = new UpdateEventPayload();

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path");

        when(storageService.getUploadUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        List<EditEventImageRequest> reqs = List.of(
                addReq(), addReq(), addReq() // > remaining slot
        );

        List<String> res = service.resolveUpdateEventImagesPayload(e, reqs, payload);

        //add 1 -> become 3 image
        assertEquals(3, res.size());
        assertEquals(MAX_IMAGES, payload.getEventImages().size());
    }

    // TC8: async multiple futures
    @Test
    void resolveUpdateEventImagesPayload_asyncUpload_shouldJoinAll() {
        Event e = eventWithImages(0);
        UpdateEventPayload payload = new UpdateEventPayload();

        when(storagePathGenerator.eventImage(any(), any(), any()))
                .thenReturn("path");

        when(storageService.getUploadUrlAsync(any()))
                .thenAnswer(inv -> CompletableFuture.completedFuture(UUID.randomUUID().toString()));

        List<String> res = service.resolveUpdateEventImagesPayload(
                e,
                List.of(addReq(), addReq(), addReq()),
                payload
        );

        assertEquals(3, res.size());
    }

}
