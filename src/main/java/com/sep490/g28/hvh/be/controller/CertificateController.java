package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.certificate.response.VerifyCertificateResponse;
import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import com.sep490.g28.hvh.be.service.CertificateService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
public class CertificateController {
    CertificateService certificateService;

    @PreAuthorize("hasRole('VOL')")
    @GetMapping("/vol/certificates")
    public ResponseEntity<Page<VolunteerCertificateResponse>> getCertificatesByVolunteer(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "INVALID_PAGE_NUMBER")
            int pageNumber,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "INVALID_PAGE_SIZE")
            @Max(value = 100, message = "INVALID_PAGE_SIZE")
            int pageSize,

            @RequestParam(required = false)
            String eventName
    ){
        return ResponseEntity.ok(certificateService.getCertificatesByVolunteer(pageNumber, pageSize, eventName));
    }

    @GetMapping("/certificates/verify/{certCode}")
    public ResponseEntity<VerifyCertificateResponse> verifyCertificate(
            @PathVariable String certCode
    ){
        return ResponseEntity.ok(certificateService.verifyCertificate(certCode));
    }
}
