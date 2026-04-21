package com.sep490.g28.hvh.be.auth.authorizer;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.repository.HostRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("hostAuthorizer")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class HostAuthorizer {

    CurrentUserProvider currentUserProvider;
    HostRepository hostRepository;

    public boolean isManagerOfHost(UUID hostId){
        UUID managerId = currentUserProvider.getId();

        return hostRepository.existsByIdAndCreatedBy_Id(hostId, managerId);
    }
}
