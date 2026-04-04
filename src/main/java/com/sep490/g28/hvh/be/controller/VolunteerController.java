package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.VolunteerRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.volunteer.response.RegisterVolunteerAccountResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationDetailsResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationSimpleResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerSimpleResponseForAdmin;
import com.sep490.g28.hvh.be.service.VolunteerService;
import com.sep490.g28.hvh.be.validation.VolunteerVerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1")
@Validated
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class VolunteerController {

    VolunteerService volunteerService;

    @PostMapping("/volunteer/register-vol-acc")
    public ResponseEntity<RegisterVolunteerAccountResponse> registerVolAccount(
            @Valid @RequestBody RegisterVolunteerAccountRequest request
    ) {
        return ResponseEntity.ok(volunteerService.registerVolAccount(request));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/volunteer/registrations")
    public ResponseEntity<Page<VolunteerRegistrationSimpleResponse>> getRegistrations(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false, name = "status")
            @VolunteerVerificationStatus
            String inputStatus,

            @RequestParam(required = false)
            String email
    ) {

        return ResponseEntity.ok(
                volunteerService.getVolRegistrations(pageNumber, pageSize, inputStatus, email)
        );
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/volunteer/registrations/{id}")
    public ResponseEntity<VolunteerRegistrationDetailsResponse> getRegistrationsDetails(@PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        return ResponseEntity.ok(volunteerService.getVolRegistrationDetails(id));
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @PostMapping("/volunteer/registrations/{id}/verify")
    public ResponseEntity<String> verifyRegistration(
            @PathVariable(name = "id") @UUID(message = "INVALID_UUID") String inputId,
            @RequestBody @Valid VolunteerRegistrationVerifyRequest request
    ) {
        java.util.UUID id = java.util.UUID.fromString(inputId);
        volunteerService.verifyVolRegistration(id, request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('SYS_ADMIN')")
    @GetMapping("/sys-admin/volunteers")
    public ResponseEntity<Page<VolunteerSimpleResponseForAdmin>> getVolunteersByAdmin(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String email
    ) {
        return ResponseEntity.ok(volunteerService.getVolunteersByAdmin(pageNumber, pageSize, email));
    }
}
