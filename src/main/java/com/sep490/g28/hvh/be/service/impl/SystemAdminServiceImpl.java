package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.orgmanager.response.OrgManagerAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.response.UpdateOrgManagerProfileResponse;
import com.sep490.g28.hvh.be.dto.systemadmin.request.UpdateSystemAdminProfileRequest;
import com.sep490.g28.hvh.be.dto.systemadmin.response.SystemAdminAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.systemadmin.response.UpdateSystemAdminProfileResponse;
import com.sep490.g28.hvh.be.entity.SystemAdmin;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SystemAdminErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.SystemAdminRepository;
import com.sep490.g28.hvh.be.service.SystemAdminService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemAdminServiceImpl implements SystemAdminService {

    SystemAdminRepository systemAdminRepository;
    StorageService storageService;

    StoragePathGenerator storagePathGenerator;
    CurrentUserProvider currentUserProvider;

    @Override
    public UpdateSystemAdminProfileResponse updateSystemAdminProfile(UpdateSystemAdminProfileRequest request) {
        UUID systemAdminId = currentUserProvider.getId();

        SystemAdmin systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new AppException(SystemAdminErrorCode.SYSTEM_ADMIN_NOT_EXISTED));

        String newAvatarUploadUrl = null;
        if(request.getAvatarExtension() != null) {
            if (systemAdmin.getAvatarUrl() != null && !systemAdmin.getAvatarUrl().isEmpty()) {
                //delete exist avatar image
                CompletableFuture<Void> avatarFuture =
                        storageService.deleteFileAsync(systemAdmin.getAvatarUrl());

                try {
                    CompletableFuture.allOf(avatarFuture).join();
                } catch (CompletionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof AppException ae && ae.getHttpStatus().value() == 400) {
                        //todo: this case is the file not exist in sb (only for test) change later, need to have picture to approve
                    } else {
                        throw (RuntimeException) e.getCause(); // propagate, transaction fail
                    }
                }
            }

            //get signed URL of file
            String newAvatarPath = storagePathGenerator.sysAdminAvatar(systemAdminId, request.getAvatarExtension());

            CompletableFuture<String> newAvatarFuture =
                    storageService.getUploadUrlAsync(newAvatarPath);

            try {
                CompletableFuture.allOf(newAvatarFuture).join();
                newAvatarUploadUrl = newAvatarFuture.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AppException ae && ae.getHttpStatus().value() == 400) {
                    //todo: this case is the file not exist in sb (only for test) change later, need to have picture to approve
                } else {
                    throw (RuntimeException) e.getCause(); // propagate, transaction fail
                }
            }

            systemAdmin.setAvatarUrl(newAvatarPath);
        }

        systemAdmin.setFullName(request.getFullName());
        systemAdmin.setGender(request.isGender());
        systemAdmin.setDob(request.getDob());
        systemAdmin.setAddress(request.getAddress());
        systemAdmin.setDetailAddress(request.getDetailAddress());
        systemAdmin.setUpdatedAt(OffsetDateTime.now());

        systemAdminRepository.save(systemAdmin);

        return UpdateSystemAdminProfileResponse.builder()
                .avatarUploadUrl(newAvatarUploadUrl)
                .build();
    }

    @Override
    public SystemAdminAccountInformationResponse getSystemAdminAccountInformation() {

        UUID systemAdminId = currentUserProvider.getId();

        SystemAdmin systemAdmin = systemAdminRepository.findById(systemAdminId)
                .orElseThrow(() -> new AppException(SystemAdminErrorCode.SYSTEM_ADMIN_NOT_EXISTED));

        //get signed URL of org manager avatar
        String avatarUrl = null;
        if (systemAdmin.getAvatarUrl() != null && !systemAdmin.getAvatarUrl().isEmpty()) {

            CompletableFuture<String> avatarFuture =
                    storageService.getSignedUrlAsync(systemAdmin.getAvatarUrl());

            try {
                CompletableFuture.allOf(avatarFuture).join();
                avatarUrl = avatarFuture.join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof AppException ae) {
                    //todo: handle app exception in viewEventFeeds
                } else {
                    throw cause instanceof RuntimeException re ? re : ex;
                }
            }
        }

        return new SystemAdminAccountInformationResponse(
                systemAdminId,
                systemAdmin.getCid(),
                systemAdmin.getEmail(),
                systemAdmin.getPhone(),
                systemAdmin.getFullName(),
                systemAdmin.getGender(),
                systemAdmin.getDob(),
                avatarUrl,
                systemAdmin.getAddress(),
                systemAdmin.getDetailAddress()
        );
    }
}
