package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.orgmanager.request.UpdateOrgManagerProfileRequest;
import com.sep490.g28.hvh.be.dto.orgmanager.response.OrgManagerAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.response.UpdateOrgManagerProfileResponse;
import com.sep490.g28.hvh.be.entity.Organization;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.service.impl.OrganizationManagerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class OrganizationManagerServiceImplTest {

    @Mock
    OrganizationManagerRepository organizationManagerRepository;
    @Mock
    StorageService storageService;

    @Mock
    StoragePathGenerator storagePathGenerator;
    @Mock
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    OrganizationManagerServiceImpl service;

    UUID orgManagerId;

    @BeforeEach
    void setUp() {

        orgManagerId = UUID.randomUUID();
    }

    private UpdateOrgManagerProfileRequest validUpdateOrgManagerProfileRequest() {
        UpdateOrgManagerProfileRequest request = new UpdateOrgManagerProfileRequest();
        request.setAvatarExtension("png");
        return request;
    }

    // ================= updateOrgManagerProfile =================
    // ===== TC1 =====
    @Test
    void updateOrgManagerProfile_success_with_avatar() {

        UUID orgManagerId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(orgManagerId);

        OrganizationManager org = new OrganizationManager();
        org.setId(orgManagerId);
        org.setAvatarUrl("old-path");

        when(organizationManagerRepository.findById(orgManagerId))
                .thenReturn(Optional.of(org));

        when(storageService.deleteFileAsync("old-path"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.orgManagerAvatar(eq(orgManagerId), any()))
                .thenReturn("new-path");

        when(storageService.getUploadUrlAsync("new-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateOrgManagerProfileRequest request = validUpdateOrgManagerProfileRequest();
        request.setAvatarExtension("png");

        UpdateOrgManagerProfileResponse res =
                service.updateOrgManagerProfile(request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-path", org.getAvatarUrl());

        verify(organizationManagerRepository).save(org);
    }

    // ===== TC2 =====
    @Test
    void updateOrgManagerProfile_not_found() {

        UUID orgManagerId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(orgManagerId);

        when(organizationManagerRepository.findById(orgManagerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.updateOrgManagerProfile(new UpdateOrgManagerProfileRequest()));
    }

    // ================= getOrgManagerAccountInformation =================
    // ===== TC1 =====
    @Test
    void getOrgManagerAccountInformation_success_full() {

        UUID orgManagerId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(orgManagerId);

        OrganizationManager org = new OrganizationManager();
        org.setCid("CID123");
        org.setEmail("mail@test.com");
        org.setPhone("0123");
        org.setFullName("Org Manager");
        org.setGender(true);
        org.setDob(LocalDate.now());
        org.setAvatarUrl("avatar-path");
        org.setAddress("addr");
        org.setDetailAddress("detail");

        when(organizationManagerRepository.findById(orgManagerId))
                .thenReturn(Optional.of(org));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        OrgManagerAccountInformationResponse res =
                service.getOrgManagerAccountInformation();

        assertEquals(orgManagerId, res.getId());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals("CID123", res.getCid());
    }

    // ===== TC2 =====
    @Test
    void getOrgManagerAccountInformation_not_found() {

        UUID orgManagerId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(orgManagerId);

        when(organizationManagerRepository.findById(orgManagerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> service.getOrgManagerAccountInformation());
    }
}
