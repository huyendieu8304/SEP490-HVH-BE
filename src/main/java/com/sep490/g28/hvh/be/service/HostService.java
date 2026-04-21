package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.response.HostActivitiesResponseForManager;
import com.sep490.g28.hvh.be.dto.host.response.HostInfoResponseForManager;
import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.UUID;

public interface HostService {
    void createHostAccount(CreateHostAccountRequest request);
    Page<HostSimpleResponseForManager> getHostsByManager(int pageNumber, int pageSize, String email);

    HostInfoResponseForManager getHostInfoByManager(UUID hostId);

    Page<HostActivitiesResponseForManager> getHostActivitiesByManager(
            UUID hostId,
            int pageNumber,
            int pageSize,
            LocalDate fromDate,
            LocalDate toDate
    );
}
