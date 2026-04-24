package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.systemadmin.request.UpdateSystemAdminProfileRequest;
import com.sep490.g28.hvh.be.dto.systemadmin.response.SystemAdminAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.systemadmin.response.UpdateSystemAdminProfileResponse;
import com.sep490.g28.hvh.be.entity.SystemAdmin;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.SystemAdminRepository;
import com.sep490.g28.hvh.be.service.impl.SystemAdminServiceImpl;
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
public class SystemAdminServiceImplTest {

    @Mock
    SystemAdminRepository systemAdminRepository;
    @Mock
    StorageService storageService;

    @Mock
    StoragePathGenerator storagePathGenerator;
    @Mock
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    SystemAdminServiceImpl service;

    UUID systemAdminId;

    @BeforeEach
    void setUp() {
        systemAdminId = UUID.randomUUID();
    }

    private UpdateSystemAdminProfileRequest validUpdateSystemAdminProfileRequest() {
        UpdateSystemAdminProfileRequest request = new UpdateSystemAdminProfileRequest();
        request.setAvatarExtension("png");
        return request;
    }

    // ================= updateSystemAdminProfile =================
    @Test
    void updateSystemAdminProfile_success_with_avatar() {

        UUID adminId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(adminId);

        SystemAdmin admin = new SystemAdmin();
        admin.setId(adminId);
        admin.setAvatarUrl("old-avatar");

        when(systemAdminRepository.findById(adminId))
                .thenReturn(Optional.of(admin));

        when(storageService.deleteFileAsync("old-avatar"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.sysAdminAvatar(eq(adminId), any()))
                .thenReturn("new-avatar-path");

        when(storageService.getUploadUrlAsync("new-avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateSystemAdminProfileRequest request = new UpdateSystemAdminProfileRequest();
        request.setAvatarExtension("png");
        request.setFullName("New Name");

        UpdateSystemAdminProfileResponse res =
                service.updateSystemAdminProfile(request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-avatar-path", admin.getAvatarUrl());

        verify(systemAdminRepository).save(admin);
    }

    // ================= getSystemAdminAccountInformation =================
    @Test
    void getSystemAdminAccountInformation_success_full() {

        UUID adminId = UUID.randomUUID();

        when(currentUserProvider.getId()).thenReturn(adminId);

        SystemAdmin admin = new SystemAdmin();
        admin.setCid("CID123");
        admin.setEmail("mail@test.com");
        admin.setPhone("0123");
        admin.setFullName("Admin");
        admin.setGender(true);
        admin.setDob(LocalDate.now());
        admin.setAvatarUrl("avatar-path");
        admin.setAddress("addr");
        admin.setDetailAddress("detail");

        when(systemAdminRepository.findById(adminId))
                .thenReturn(Optional.of(admin));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        SystemAdminAccountInformationResponse res =
                service.getSystemAdminAccountInformation();

        assertEquals(adminId, res.getId());
        assertEquals("signed-url", res.getAvatarUrl());
        assertEquals("CID123", res.getCid());
    }
}
