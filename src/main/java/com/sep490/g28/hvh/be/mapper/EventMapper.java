package com.sep490.g28.hvh.be.mapper;

import com.sep490.g28.hvh.be.dto.event.response.EventSimpleResponseForAdmin;
import com.sep490.g28.hvh.be.dto.event.response.EventSimpleResponseForManager;
import com.sep490.g28.hvh.be.entity.Event;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventSimpleResponseForManager toEventSimpleResponseForManager(Event event) {
        EventSimpleResponseForManager response = new EventSimpleResponseForManager();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setAddress(event.getAddress());
        response.setStartDate(event.getStartDate());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setStatus(event.getStatus());
        return response;
    }

    public EventSimpleResponseForAdmin toEventSimpleResponseForAdmin(Event event) {
        EventSimpleResponseForAdmin response = new EventSimpleResponseForAdmin();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setOrganizationId(event.getOrganization().getId());
        response.setOrganizationName(event.getOrganization().getName());
        response.setAddress(event.getAddress());
        response.setStartDate(event.getStartDate());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setStatus(event.getStatus());
        return response;
    }
}
