package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.host.response.HostAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.request.UpdateOrgManagerProfileRequest;
import com.sep490.g28.hvh.be.dto.orgmanager.response.OrgManagerAccountInformationResponse;
import com.sep490.g28.hvh.be.dto.orgmanager.response.UpdateOrgManagerProfileResponse;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.OrganizationManagerErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.service.OrganizationManagerService;
import com.sep490.g28.hvh.be.util.AsyncExceptionUtils;
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
public class OrganizationManagerServiceImpl implements OrganizationManagerService {

    OrganizationManagerRepository organizationManagerRepository;
    StorageService storageService;

    StoragePathGenerator storagePathGenerator;
    CurrentUserProvider currentUserProvider;

    @Override
    public UpdateOrgManagerProfileResponse updateOrgManagerProfile(UpdateOrgManagerProfileRequest request) {
        UUID orgManagerId = currentUserProvider.getId();

        OrganizationManager orgManager = organizationManagerRepository.findById(orgManagerId)
                .orElseThrow(() -> new AppException(OrganizationManagerErrorCode.ORGANIZATION_MANAGER_NOT_EXISTED));

        String newAvatarUploadUrl = null;
        if(request.getAvatarExtension() != null) {
            if (orgManager.getAvatarUrl() != null && !orgManager.getAvatarUrl().isEmpty()) {
                //delete exist avatar image
                CompletableFuture<Void> avatarFuture =
                        storageService.deleteFileAsync(orgManager.getAvatarUrl());

                try {
                    CompletableFuture.allOf(avatarFuture).join();
                } catch (CompletionException ex) {
                    AsyncExceptionUtils.resolveExceptionIgnoreIfFileNotExisted(ex);
                }
            }

            //get signed URL of file
            String newAvatarPath = storagePathGenerator.orgManagerAvatar(orgManagerId, request.getAvatarExtension());

            CompletableFuture<String> newAvatarFuture =
                    storageService.getUploadUrlAsync(newAvatarPath);

            try {
                CompletableFuture.allOf(newAvatarFuture).join();
                newAvatarUploadUrl = newAvatarFuture.join();
            } catch (CompletionException ex) {
                AsyncExceptionUtils.resolveExceptionIgnoreIfFileNotExisted(ex);
            }

            orgManager.setAvatarUrl(newAvatarPath);
        }

        orgManager.setFullName(request.getFullName());
        orgManager.setGender(request.isGender());
        orgManager.setDob(request.getDob());
        orgManager.setAddress(request.getAddress());
        orgManager.setDetailAddress(request.getDetailAddress());
        orgManager.setUpdatedAt(OffsetDateTime.now());

        organizationManagerRepository.save(orgManager);

        return UpdateOrgManagerProfileResponse.builder()
                .avatarUploadUrl(newAvatarUploadUrl)
                .build();
    }

    @Override
    public OrgManagerAccountInformationResponse getOrgManagerAccountInformation() {

        UUID orgManagerId = currentUserProvider.getId();

        OrganizationManager orgManager = organizationManagerRepository.findById(orgManagerId)
                .orElseThrow(() -> new AppException(OrganizationManagerErrorCode.ORGANIZATION_MANAGER_NOT_EXISTED));

        //get signed URL of org manager avatar
        String avatarUrl = null;
        if (orgManager.getAvatarUrl() != null && !orgManager.getAvatarUrl().isEmpty()) {

            CompletableFuture<String> avatarFuture =
                    storageService.getSignedUrlAsync(orgManager.getAvatarUrl());

            try {
                CompletableFuture.allOf(avatarFuture).join();
                avatarUrl = avatarFuture.join();
            } catch (CompletionException ex) {
                AsyncExceptionUtils.resolveExceptionIgnoreIfFileNotExisted(ex);
            }
        }

        return new OrgManagerAccountInformationResponse(
                orgManagerId,
                orgManager.getCid(),
                orgManager.getEmail(),
                orgManager.getPhone(),
                orgManager.getFullName(),
                orgManager.getGender(),
                orgManager.getDob(),
                avatarUrl,
                orgManager.getAddress(),
                orgManager.getDetailAddress()
        );
    }
}
