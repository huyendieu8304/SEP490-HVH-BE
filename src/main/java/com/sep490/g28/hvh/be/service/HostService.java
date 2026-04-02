package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import org.springframework.data.domain.Page;

public interface HostService {
    void createHostAccount(CreateHostAccountRequest request);
    Page<HostSimpleResponseForManager> getHostsByManager(int pageNumber, int pageSize, String email);
}
