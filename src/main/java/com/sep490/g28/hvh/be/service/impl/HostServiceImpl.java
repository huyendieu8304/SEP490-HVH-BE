package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.request.UpdateHostProfileBySystemAdminRequest;
import com.sep490.g28.hvh.be.dto.host.request.UpdateHostProfileRequest;
import com.sep490.g28.hvh.be.dto.host.response.*;
import com.sep490.g28.hvh.be.entity.Host;
import com.sep490.g28.hvh.be.entity.Organization;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.HostErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.HostService;
import com.sep490.g28.hvh.be.util.AsyncExceptionUtils;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.sep490.g28.hvh.be.util.StringNormalizeUtil.normalizeVietnameseName;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HostServiceImpl implements HostService {
    HostRepository hostRepository;
    OrganizationManagerRepository organizationManagerRepository;
    UserRepository userRepository;

    AuthClient authClient;
    EmailService emailService;
    StorageService storageService;

    StoragePathGenerator storagePathGenerator;
    CurrentUserProvider currentUserProvider;

    @Transactional
    @Override
    public void createHostAccount(CreateHostAccountRequest request) {
        OrganizationManager organizationManager = organizationManagerRepository.getReferenceById(currentUserProvider.getId());

        //check whether email used by any account
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HostErrorCode.EMAIL_USED);
        }
        //create account in supabase
        String defaultPassword = RandomStringUtil.random8AlphaNumeric();
        //create host account in auth server
        UUID hostId = authClient.createAccount(ERole.HOST, request.getEmail(), defaultPassword, request.getPhone());

        Host host = new Host();
        host.setId(hostId);
        host.setCid(request.getCid());
        host.setEmail(request.getEmail());
        host.setPhone(request.getPhone());
        host.setFullName(normalizeVietnameseName(request.getFullName()));
        host.setAddress(request.getAddress());
        host.setDetailAddress(request.getDetailAddress());
        host.setCreatedBy(organizationManager);
        host.setOrganization(organizationManager.getOrganization());

        //save to db
        hostRepository.save(host);
        log.info("Create host account, id={}", hostId);

        //send mail
        emailService.sendCreateHostAccountEmail(
                organizationManager.getOrganization().getName(),
                request.getEmail(),
                defaultPassword
        );
    }

    @Override
    public Page<HostSimpleResponseForManager> getHostsByManager(int pageNumber, int pageSize, String email) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<HostSimpleResponseForManager> page =
                hostRepository.getHostsByManager(currentUserProvider.getId(), pageable, email);
        //get avatar signed urls
        List<HostSimpleResponseForManager> content =
                page.getContent().stream()
                        .map(host -> {
                            if (host.getAvatarUrl() == null) return host;
                            //get signed url for volunteer avatar
                            String path = host.getAvatarUrl();
                            try {
                                String url = storageService.getSignedUrlAsync(path).join();
                                host.setAvatarUrl(url);
                            } catch (CompletionException e) {
                                host.setAvatarUrl(
                                        AsyncExceptionUtils.resolveExceptionReturnFallbackIfFileNotExisted(e, null)
                                );
                            }
                            return host;
                        })
                        .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public HostInfoResponseForManager getHostInfoByManager(UUID hostId) {

        Host host = hostRepository.findById(hostId).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        HostInfoResponseForManager response = new HostInfoResponseForManager();
        response.setId(host.getId());
        response.setCid(host.getCid());
        response.setEmail(host.getEmail());
        response.setPhone(host.getPhone());
        response.setFullName(host.getFullName());
        response.setGender(host.getGender());
        response.setDob(host.getDob());

        try {
            String avatarUrl = storageService.getSignedUrl(host.getAvatarUrl());
            response.setAvatarUrl(avatarUrl);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof AppException ae
                    && ae.getCode() != SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED.getCode()) throw ex;
        }

        response.setAddress(host.getAddress());
        response.setDetailAddress(host.getDetailAddress());
        response.setCreatedAt(host.getCreatedAt());

        return response;
    }

    @Override
    public Page<HostActivitiesResponseForManager> getHostActivitiesByManager(
            UUID hostId,
            int pageNumber,
            int pageSize,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

        OffsetDateTime from = fromDate.atStartOfDay(vnZone)
                    .toOffsetDateTime();
//                    .withOffsetSameInstant(ZoneOffset.UTC); converter would do this when create query

        OffsetDateTime to = toDate.atTime(LocalTime.MAX)
                    .atZone(vnZone)
                    .toOffsetDateTime();
//                    .withOffsetSameInstant(ZoneOffset.UTC); converter would do this when create query

        return hostRepository.getHostActivitiesByManager(hostId, pageable, from, to);
    }

    @Override
    public UpdateHostProfileResponse updateHostProfile(UpdateHostProfileRequest request) {

        UUID hostId = currentUserProvider.getId();

        Host host = hostRepository.findById(hostId).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        String newAvatarUploadUrl = null;
        if(request.getAvatarExtension() != null) {
            if (host.getAvatarUrl() != null && !host.getAvatarUrl().isEmpty()) {
                //delete exist avatar image
                CompletableFuture<Void> avatarFuture =
                        storageService.deleteFileAsync(host.getAvatarUrl());

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
            String newAvatarPath = storagePathGenerator.hostAvatar(hostId, request.getAvatarExtension());

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

            host.setAvatarUrl(newAvatarPath);
        }

        host.setFullName(request.getFullName());
        host.setGender(request.isGender());
        host.setDob(request.getDob());
        host.setAddress(request.getAddress());
        host.setDetailAddress(request.getDetailAddress());
        host.setUpdatedAt(OffsetDateTime.now());

        hostRepository.save(host);

        return UpdateHostProfileResponse.builder()
                .avatarUploadUrl(newAvatarUploadUrl)
                .build();
    }

    @Override
    public HostAccountInformationResponse getHostAccountInformation() {

        UUID hostId = currentUserProvider.getId();

        Host host = hostRepository.findById(hostId).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        //get signed URL of host avatar
        String avatarUrl = null;
        if (host.getAvatarUrl() != null && !host.getAvatarUrl().isEmpty()) {

            CompletableFuture<String> avatarFuture =
                    storageService.getSignedUrlAsync(host.getAvatarUrl());

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

        return new HostAccountInformationResponse(
                hostId,
                host.getCid(),
                host.getEmail(),
                host.getPhone(),
                host.getFullName(),
                host.getGender(),
                host.getDob(),
                avatarUrl,
                host.getAddress(),
                host.getDetailAddress()
        );
    }

    @Override
    public Page<HostSimpleResponseForSystemAdmin> getHostsOfOrganizationBySystemAdmin(int pageNumber, int pageSize, String email) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<HostSimpleResponseForSystemAdmin> page =
                hostRepository.getHostsOfOrganizationBySystemAdmin(pageable, email);
        //get avatar signed urls
        List<HostSimpleResponseForSystemAdmin> content =
                page.getContent().stream()
                        .map(host -> {
                            if (host.getAvatarUrl() == null) return host;
                            //get signed url for host avatar
                            String path = host.getAvatarUrl();
                            try {
                                String url = storageService.getSignedUrlAsync(path).join();
                                host.setAvatarUrl(url);
                            } catch (CompletionException e) {
                                host.setAvatarUrl(
                                        AsyncExceptionUtils.resolveExceptionReturnFallbackIfFileNotExisted(e, null)
                                );
                            }
                            return host;
                        })
                        .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public HostInfoResponseForSystemAdmin getHostInfoBySystemAdmin(UUID hostId) {
        Host host = hostRepository.findById(hostId).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        HostInfoResponseForSystemAdmin response = new HostInfoResponseForSystemAdmin();
        response.setId(host.getId());
        response.setCid(host.getCid());
        response.setEmail(host.getEmail());
        response.setPhone(host.getPhone());
        response.setFullName(host.getFullName());
        response.setGender(host.getGender());
        response.setDob(host.getDob());

        try {
            String avatarUrl = storageService.getSignedUrl(host.getAvatarUrl());
            response.setAvatarUrl(avatarUrl);
        } catch (AppException e) {
            response.setAvatarUrl(null);
        }

        response.setAddress(host.getAddress());
        response.setDetailAddress(host.getDetailAddress());
        response.setCreatedAt(host.getCreatedAt());

        Organization organization = host.getOrganization();

        if(organization != null) {
            try {
                String orgAvatarUrl = storageService.getSignedUrl(organization.getAvatarImage());
                response.setOrgAvatarUrl(orgAvatarUrl);
            } catch (AppException e) {
                response.setOrgAvatarUrl(null);
            }

            response.setOrgId(organization.getId());
            response.setOrgName(organization.getName());
            response.setOrgAvgRating(organization.getAvgRating());
            response.setOrgHostedEventCount(organization.getHostedEventCount());
        }

        return response;
    }

    @Override
    public Page<HostActivitiesResponseForSystemAdmin> getHostActivitiesBySystemAdmin(UUID hostId, int pageNumber, int pageSize, LocalDate fromDate, LocalDate toDate) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");

        OffsetDateTime from = fromDate.atStartOfDay(vnZone)
                .toOffsetDateTime();
//                    .withOffsetSameInstant(ZoneOffset.UTC); converter would do this when create query

        OffsetDateTime to = toDate.atTime(LocalTime.MAX)
                .atZone(vnZone)
                .toOffsetDateTime();
//                    .withOffsetSameInstant(ZoneOffset.UTC); converter would do this when create query

        return hostRepository.getHostActivitiesBySystemAdmin(hostId, pageable, from, to);
    }

    @Override
    public UpdateHostProfileResponse updateHostProfileBySystemAdmin(UUID hostId, UpdateHostProfileBySystemAdminRequest request) {

        Host host = hostRepository.findById(hostId).orElseThrow(
                () -> new AppException(HostErrorCode.HOST_NOT_EXISTED)
        );

        String newAvatarUploadUrl = null;
        if(request.getAvatarExtension() != null) {
            if (host.getAvatarUrl() != null && !host.getAvatarUrl().isEmpty()) {
                //delete exist avatar image
                CompletableFuture<Void> avatarFuture =
                        storageService.deleteFileAsync(host.getAvatarUrl());

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
            String newAvatarPath = storagePathGenerator.hostAvatar(hostId, request.getAvatarExtension());

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

            host.setAvatarUrl(newAvatarPath);
        }

        host.setCid(request.getCid());
        host.setPhone(request.getPhone());
        host.setEmail(request.getEmail());
        host.setFullName(request.getFullName());
        host.setGender(request.isGender());
        host.setDob(request.getDob());
        host.setAddress(request.getAddress());
        host.setDetailAddress(request.getDetailAddress());
        host.setUpdatedAt(OffsetDateTime.now());

        hostRepository.save(host);

        return UpdateHostProfileResponse.builder()
                .avatarUploadUrl(newAvatarUploadUrl)
                .build();
    }
}
