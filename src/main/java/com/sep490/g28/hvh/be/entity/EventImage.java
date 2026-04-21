package com.sep490.g28.hvh.be.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "event_images",
        indexes = {
                @Index(name = "idx_event_images_event_id", columnList = "event_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventImage {
    @Id
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event event;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    public EventImage(EventImage eventImage) {
        this.id = eventImage.getId();
        this.event = eventImage.getEvent();
        this.imagePath = eventImage.getImagePath();
    }
}
