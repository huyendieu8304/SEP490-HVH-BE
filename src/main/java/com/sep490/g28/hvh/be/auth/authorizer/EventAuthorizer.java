package com.sep490.g28.hvh.be.auth.authorizer;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("eventAuthorizer")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EventAuthorizer {

    EventRepository eventRepository;
    OrganizationManagerRepository organizationManagerRepository;
    CurrentUserProvider currentUserProvider;

    public boolean isHostOfEvent(UUID eventId){

        UUID hostId = currentUserProvider.getId();
        return eventRepository.existsByIdAndHost_Id(eventId, hostId);
    }

    public boolean isOrgManagerOfEvent(UUID eventId){
        UUID managerId = currentUserProvider.getId();

        OrganizationManager organizationManager = organizationManagerRepository.findById(managerId).orElseThrow(
                () -> new AppException(AppCommonErrorCode.UNAUTHENTICATED)
        );

        UUID organizationId = organizationManager.getOrganization().getId();

        return eventRepository.existsByIdAndOrganization_Id(eventId, organizationId);
    }

}
