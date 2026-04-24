package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.UpdateVolunteerProfileBySystemAdminRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.UpdateVolunteerProfileRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.VolunteerRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.volunteer.response.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface VolunteerService {
    RegisterVolunteerAccountResponse registerVolAccount (RegisterVolunteerAccountRequest registerVolunteerAccountRequest);

    Page<VolunteerRegistrationSimpleResponse> getVolRegistrations(int pageNumber, int pageSize, String inputStatus, String email);

    VolunteerRegistrationDetailsResponse getVolRegistrationDetails(UUID id);

    void verifyVolRegistration(UUID id, VolunteerRegistrationVerifyRequest request);

    Page<VolunteerSimpleResponseForAdmin> getVolunteersByAdmin(int pageNumber, int pageSize, String email);

    Page<VolunteerActivitiesResponseForAdmin> getVolunteerActivitiesByAdmin(UUID id, int pageNumber, int pageSize);

    VolunteerPublicInformationResponse getVolunteerPublicInformation(UUID volunteerId);

    VolunteerAccountInformationResponse getVolunteerAccountInformation();

    void registerVolunteerFace(String deviceId, MultipartFile file);

    UpdateVolunteerProfileResponse updateVolunteerProfile(UpdateVolunteerProfileRequest request);

    UpdateVolunteerProfileResponse updateVolunteerProfileBySystemAdmin(UUID volunteerId, UpdateVolunteerProfileBySystemAdminRequest request);

    VolunteerAccountInformationResponse getVolunteerAccountInformationByAdmin(UUID volunteerId);
}
