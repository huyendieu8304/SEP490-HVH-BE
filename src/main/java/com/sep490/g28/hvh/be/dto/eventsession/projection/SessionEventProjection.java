package com.sep490.g28.hvh.be.dto.eventsession.projection;

import com.sep490.g28.hvh.be.entity.EventSession;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SessionEventProjection {
    private EventSession session;
    private UUID eventId;
    private String eventName;
    private UUID hostId;

    public SessionEventProjection(
            EventSession session,
            UUID eventId,
            String eventName,
            UUID hostId
    ) {
        this.session = session;
        this.eventId = eventId;
        this.eventName = eventName;
        this.hostId = hostId;
    }
}
