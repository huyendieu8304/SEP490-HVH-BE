package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.VolunteerRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.volunteer.response.RegisterVolunteerAccountResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationDetailsResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationSimpleResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerSimpleResponseForAdmin;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface VolunteerService {
    RegisterVolunteerAccountResponse registerVolAccount (RegisterVolunteerAccountRequest registerVolunteerAccountRequest);

    Page<VolunteerRegistrationSimpleResponse> getVolRegistrations(int pageNumber, int pageSize, String inputStatus, String email);

    VolunteerRegistrationDetailsResponse getVolRegistrationDetails(UUID id);

    void verifyVolRegistration(UUID id, VolunteerRegistrationVerifyRequest request);

    Page<VolunteerSimpleResponseForAdmin> getVolunteersByAdmin(int pageNumber, int pageSize, String email);
}
