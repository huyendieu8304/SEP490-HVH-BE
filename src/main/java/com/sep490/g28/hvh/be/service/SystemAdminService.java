package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.systemadmin.request.UpdateSystemAdminProfileRequest;
import com.sep490.g28.hvh.be.dto.systemadmin.response.SystemAdminAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.systemadmin.response.UpdateSystemAdminProfileResponse;

public interface SystemAdminService {
    UpdateSystemAdminProfileResponse updateSystemAdminProfile(UpdateSystemAdminProfileRequest request);

    SystemAdminAccountInformationResponse getSystemAdminAccountInformation();
}
