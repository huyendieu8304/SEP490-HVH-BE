package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.orgmanager.request.UpdateOrgManagerProfileRequest;
import com.sep490.g28.hvh.be.dto.orgmanager.response.OrgManagerAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.response.UpdateOrgManagerProfileResponse;

public interface OrganizationManagerService {
    UpdateOrgManagerProfileResponse updateOrgManagerProfile(UpdateOrgManagerProfileRequest request);

    OrgManagerAccountInformationResponse getOrgManagerAccountInformation();
}
