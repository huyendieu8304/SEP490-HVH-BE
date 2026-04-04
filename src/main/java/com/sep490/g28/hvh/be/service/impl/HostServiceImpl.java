package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.dto.host.response.HostActivitiesResponse;
import com.sep490.g28.hvh.be.dto.host.response.HostInfoResponseForManager;
import com.sep490.g28.hvh.be.dto.host.response.HostSimpleResponseForManager;
import com.sep490.g28.hvh.be.entity.Host;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.HostErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import com.sep490.g28.hvh.be.service.HostService;
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

    //todo unit test
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
        List<CompletableFuture<HostSimpleResponseForManager>> futures =
                page.getContent().stream()
                        .map(h -> {
                            if (h.getAvatarUrl() == null) {
                                return CompletableFuture.completedFuture(h);
                            }
                            return storageService.getSignedUrlAsync(h.getAvatarUrl())
                                    .thenApply(url -> {
                                        h.setAvatarUrl(url);
                                        return h;
                                    })
                                    //todo this might be put into some todos
                                    .exceptionally(ex -> {
                                        log.warn("Failed to get signed url for path: {}", h.getAvatarUrl(), ex);
                                        h.setAvatarUrl(null);
                                        return h;
                                    });
                        })
                        .toList();
        List<HostSimpleResponseForManager> content =
                futures.stream().map(CompletableFuture::join).toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    //todo unit test
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
        } catch (AppException e) {
            response.setAvatarUrl(null);
        }

        response.setAddress(host.getAddress());
        response.setDetailAddress(host.getDetailAddress());
        response.setCreatedAt(host.getCreatedAt());

        return response;
    }

    //todo unit test
    @Override
    public Page<HostActivitiesResponse> getHostActivitiesByManager(
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
                    .toOffsetDateTime()
                    .withOffsetSameInstant(ZoneOffset.UTC);

        OffsetDateTime to = toDate.atTime(LocalTime.MAX)
                    .atZone(vnZone)
                    .toOffsetDateTime()
                    .withOffsetSameInstant(ZoneOffset.UTC);

        return hostRepository.getHostActivitiesByManager(hostId, pageable, from, to);
    }
}
