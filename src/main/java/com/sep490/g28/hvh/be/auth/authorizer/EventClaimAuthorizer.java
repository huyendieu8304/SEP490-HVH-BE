package com.sep490.g28.hvh.be.auth.authorizer;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.repository.EventClaimRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("eventClaimAuthorizer")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EventClaimAuthorizer {

    EventClaimRepository eventClaimRepository;
    CurrentUserProvider currentUserProvider;

    public boolean isEventClaimManagedByHost(UUID claimId) {
        UUID hostId = currentUserProvider.getId();

        return eventClaimRepository.existsByIdAndHost_Id(claimId, hostId);
    }
}
