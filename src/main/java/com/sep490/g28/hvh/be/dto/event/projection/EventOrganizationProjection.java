package com.sep490.g28.hvh.be.dto.event.projection;

import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Organization;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventOrganizationProjection {
    private Event event;
    private Organization organization;

    public EventOrganizationProjection(Event event, Organization organization) {
        this.event = event;
        this.organization = organization;
    }
}
