package com.sep490.g28.hvh.be.auth.authorizer;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.repository.EventApplicationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("eventApplicationAuthorizer")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EventApplicationAuthorizer {

    EventApplicationRepository eventApplicationRepository;
    CurrentUserProvider currentUserProvider;

    public boolean isHostOfEventApplication(UUID applicationId){
        UUID hostId = currentUserProvider.getId();

        return eventApplicationRepository.existsByIdAndHostId(applicationId, hostId);
    }
}
