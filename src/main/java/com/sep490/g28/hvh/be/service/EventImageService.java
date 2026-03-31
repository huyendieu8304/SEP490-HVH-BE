package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventImagePayload;
import com.sep490.g28.hvh.be.dto.event.payload.UpdateEventPayload;
import com.sep490.g28.hvh.be.dto.eventimage.request.EditEventImageRequest;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.EventImage;

import java.util.List;

public interface EventImageService {

    List<String> addEventImages(Event event, List<EditEventImageRequest> addImages);
    List<String> updateEventImages(Event event, List<EditEventImageRequest> reqImages);
    List<String> resolveUpdateEventImages(
            Event event,
            List<EditEventImageRequest> reqImages,
            UpdateEventPayload updateEventPayload
    );

    List<EventImage> resolveUpdatedEventImages(
            Event event,
            List<EventImage> oldImages,
            List<UpdateEventImagePayload> updateEventImagePayloads
    );
}
