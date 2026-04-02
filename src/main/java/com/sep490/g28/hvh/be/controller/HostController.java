package com.sep490.g28.hvh.be.controller;

import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import com.sep490.g28.hvh.be.service.HostService;
import jakarta.validation.Valid;
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
public class HostController {

    HostService hostService;

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @PostMapping("/host/create-account")
    public ResponseEntity<Void> createAccount(
            @RequestBody @Valid CreateHostAccountRequest request
    ) {
        hostService.createHostAccount(request);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ORG_MANAGER')")
    @PostMapping("/org-manager/hosts")
    public ResponseEntity<Page<HostSimpleResponseForManager>> getHostsByManager(
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
        return ResponseEntity.ok(hostService.getHostsByManager(pageNumber, pageSize, email));
    }

}
