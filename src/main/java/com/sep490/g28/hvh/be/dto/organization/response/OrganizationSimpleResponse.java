package com.sep490.g28.hvh.be.dto.organization.response;

import com.sep490.g28.hvh.be.constant.EOrgType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrganizationSimpleResponse {
    private UUID id;
    private String name;
    private EOrgType orgType;
    private long numberOfHostedEvents;
    private long creditHour;
    //todo add rating
}
