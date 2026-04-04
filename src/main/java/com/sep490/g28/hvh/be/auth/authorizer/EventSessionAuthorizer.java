package com.sep490.g28.hvh.be.auth.authorizer;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.repository.EventSessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("eventSessionAuthorizer")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EventSessionAuthorizer {

    EventSessionRepository eventSessionRepository;
    CurrentUserProvider currentUserProvider;


    public boolean isHostOfEventSession(UUID eventSessionId) {
        return eventSessionRepository.existsByIdAndAndEvent_Host_Id(eventSessionId, currentUserProvider.getId());
    }
}
