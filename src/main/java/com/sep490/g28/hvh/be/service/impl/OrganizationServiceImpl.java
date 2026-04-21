package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EOrgRegistrationStatus;
import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.constant.EOrganizationStatus;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.dto.event.projection.EventOrganizationProjection;
import com.sep490.g28.hvh.be.dto.organization.request.OrganizationRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.organization.request.RegisterOrganizationRequest;
import com.sep490.g28.hvh.be.dto.organization.response.*;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.OrganizationErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.mapper.OrganizationMapper;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.OrganizationService;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrganizationServiceImpl implements OrganizationService {

    OrganizationRegistrationRepository organizationRegistrationRepository;
    OrganizationRepository organizationRepository;
    OrganizationManagerRepository organizationManagerRepository;
    UserRepository userRepository;
    StorageService storageService;
    StoragePathGenerator storagePathGenerator;
    OtpService otpService;
    SystemAdminRepository systemAdminRepository;
    CurrentUserProvider currentUserProvider;
    AuthClient authClient;
    EmailService emailService;
    HostRepository hostRepository;
    EventRepository eventRepository;
    OrganizationMapper organizationMapper;

    @Override
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {

        //1. validate otp
        otpService.verifyOrgRegistrationOtp(request.getManagerEmail(), request.getOtp());

        //2. check the unique email in the all system's account
        if (userRepository.existsByEmail(request.getManagerEmail())) {
            throw new AppException(OrganizationErrorCode.EMAIL_USED);
        }

        OrganizationRegistration orgRegistration = new OrganizationRegistration();
        UUID id = UUID.randomUUID();
        orgRegistration.setId(id);
        orgRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        //3. generate upload url for fe
        //get the path in storage
        String managerCidFrontPath = storagePathGenerator.orgRegistrationCidFront(id, request.getManagerCidFrontExtension());
        String managerCidBackPath = storagePathGenerator.orgRegistrationCidBack(id, request.getManagerCidBackExtension());
        String managerCidHoldingPath = storagePathGenerator.orgRegistrationCidHolding(id, request.getManagerCidHoldingExtension());

        String[] legalDocuments = request.getLegalDocumentsExtensions().split("\\s+");
        List<String> legalDocumentsPathsList = new ArrayList<>();
        int legal_order = 1;
        for (String legalDocument : legalDocuments) {
            String otherEvidencePath = storagePathGenerator.orgRegistrationLegalDocuments(id, legal_order++, legalDocument);
            legalDocumentsPathsList.add(otherEvidencePath);
            if (legal_order == 11) {
                break;
            }
        }

        StringBuilder legalDocumentsPathsSB = new StringBuilder();
        for (String legalDocumentPath : legalDocumentsPathsList) {
            legalDocumentsPathsSB.append(legalDocumentPath).append(" ");
        }
        String legalDocumentsPaths = legalDocumentsPathsSB.toString().trim();

        String otherEvidencesPaths = "";
        List<String> otherEvidencesPathsList = new ArrayList<>();
        if(request.getOtherEvidencesExtensions() != null) {
            String[] otherEvidences = request.getOtherEvidencesExtensions().split("\\s+");
            int order = 1;
            for (String otherEvidence : otherEvidences) {
                String otherEvidencePath = storagePathGenerator.orgRegistrationOtherEvidences(id, order++, otherEvidence);
                otherEvidencesPathsList.add(otherEvidencePath);
                if (order == 6) {
                    break;
                }
            }
            StringBuilder otherEvidencesPathsSB = new StringBuilder();
            for (String otherEvidencePath : otherEvidencesPathsList) {
                otherEvidencesPathsSB.append(otherEvidencePath).append(" ");
            }
            otherEvidencesPaths = otherEvidencesPathsSB.toString().trim();
        }

        //get upload url
        CompletableFuture<String> managerCidFrontFuture =
                storageService.getUploadUrlAsync(managerCidFrontPath);
        CompletableFuture<String> managerCidBackFuture =
                storageService.getUploadUrlAsync(managerCidBackPath);
        CompletableFuture<String> managerCidHoldingFuture =
                storageService.getUploadUrlAsync(managerCidHoldingPath);

        List<CompletableFuture<String>> legalDocumentsFutures = new ArrayList<>();
        for (String legalDocumentPath : legalDocumentsPathsList) {
            CompletableFuture<String> legalDocumentFuture =
                    storageService.getUploadUrlAsync(legalDocumentPath);
            legalDocumentsFutures.add(legalDocumentFuture);
        }

        List<CompletableFuture<String>> otherEvidencesFutures = new ArrayList<>();
        if(!otherEvidencesPathsList.isEmpty()) {
            for (String otherEvidencePath : otherEvidencesPathsList) {
                CompletableFuture<String> otherEvidenceFuture =
                        storageService.getUploadUrlAsync(otherEvidencePath);
                otherEvidencesFutures.add(otherEvidenceFuture);
            }
        }

        try {
            CompletableFuture.allOf(managerCidFrontFuture, managerCidBackFuture, managerCidHoldingFuture).join();
            CompletableFuture.allOf(legalDocumentsFutures.toArray(new CompletableFuture[0])).join();
            if(!otherEvidencesFutures.isEmpty()) {
                CompletableFuture.allOf(otherEvidencesFutures.toArray(new CompletableFuture[0])).join();
            }
        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }

        String managerCidFrontUploadUrl = managerCidFrontFuture.join();
        String managerCidBackUploadUrl = managerCidBackFuture.join();
        String managerCidHoldingUploadUrl = managerCidHoldingFuture.join();

        List<String> legalDocumentsUploadUrl = new ArrayList<>();
        for (CompletableFuture<String> legalDocumentsFuture : legalDocumentsFutures) {
            legalDocumentsUploadUrl.add(legalDocumentsFuture.join());
        }

        List<String> otherEvidencesUploadUrl = new ArrayList<>();
        if(!otherEvidencesFutures.isEmpty()) {
            for (CompletableFuture<String> otherEvidenceFuture : otherEvidencesFutures) {
                otherEvidencesUploadUrl.add(otherEvidenceFuture.join());
            }
        }

        //4. create organization registration in db
        orgRegistration.setName(request.getName());
        orgRegistration.setDhaRegistered(request.getDhaRegistered());
        orgRegistration.setOrgType(EOrgType.valueOf(request.getOrgType()));
        orgRegistration.setOrgIntroduction(request.getOrgIntroduction());
        orgRegistration.setManagerFullName(request.getManagerFullName());
        orgRegistration.setManagerCid(request.getManagerCid());
        orgRegistration.setManagerPhone(request.getManagerPhone());
        orgRegistration.setManagerEmail(request.getManagerEmail());
        orgRegistration.setApplicationReason(request.getApplicationReason());

        orgRegistration.setManagerCidFront(managerCidFrontPath);
        orgRegistration.setManagerCidBack(managerCidBackPath);
        orgRegistration.setManagerCidHolding(managerCidHoldingPath);
        orgRegistration.setLegalDocument(legalDocumentsPaths);
        orgRegistration.setOtherEvidences(otherEvidencesPaths);

        organizationRegistrationRepository.save(orgRegistration);
        log.info("Create new organization registration: {}", orgRegistration.getId());

        return RegisterOrganizationResponse.builder()
                .managerCidFrontUploadUrl(managerCidFrontUploadUrl)
                .managerCidBackUploadUrl(managerCidBackUploadUrl)
                .managerCidHoldingUploadUrl(managerCidHoldingUploadUrl)
                .legalDocumentsUploadUrls(legalDocumentsUploadUrl)
                .otherEvidencesUploadUrls(otherEvidencesUploadUrl)
                .build();
    }

    @Override
    public Page<OrganizationRegistrationSimpleResponse> getOrgRegistrations(int pageNumber, int pageSize, String inputStatus, String managerEmail) {
        //parse status
        EOrgRegistrationStatus status =
                (inputStatus == null || inputStatus.isBlank())
                        ? null
                        : EOrgRegistrationStatus.valueOf(inputStatus);

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        //search
        return organizationRegistrationRepository.search(status, managerEmail, pageable)
                .map(OrganizationRegistrationSimpleResponse::from);
    }

    @Override
    public OrganizationRegistrationDetailsResponse getOrgRegistrationDetails(UUID id) {
        //check id exist
        OrganizationRegistration organizationRegistration = organizationRegistrationRepository.findById(id).orElseThrow(
                () -> new AppException(OrganizationErrorCode.REGISTRATION_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //check email exist in any account
        if (userRepository.existsByEmail(organizationRegistration.getManagerEmail())) {
            //add to the note to announce sys_admin
            note.append(OrganizationErrorCode.EMAIL_USED.getMessage()).append("\n");
        }

        //build response
        OrganizationRegistrationDetailsResponse response = OrganizationRegistrationDetailsResponse.builder()
                .id(organizationRegistration.getId())
                .name(organizationRegistration.getName())
                .dhaRegistered(organizationRegistration.getDhaRegistered())
                .orgType(organizationRegistration.getOrgType())
                .orgIntroduction(organizationRegistration.getOrgIntroduction())
                .managerFullName(organizationRegistration.getManagerFullName())
                .managerCid(organizationRegistration.getManagerCid())
                .managerPhone(organizationRegistration.getManagerPhone())
                .managerEmail(organizationRegistration.getManagerEmail())
                .applicationReason(organizationRegistration.getApplicationReason())
                .status(organizationRegistration.getStatus())
                .rejectionReason(organizationRegistration.getRejectionReason())
                .createdAt(organizationRegistration.getCreatedAt())
                .reviewedAt(organizationRegistration.getReviewedAt())
                .build();

        if(organizationRegistration.getStatus() != EOrgRegistrationStatus.PENDING) {
            response.setAdminId(organizationRegistration.getReviewedBy().getId());
            response.setOrganizationId(organizationRegistration.getOrganization().getId());
            response.setOrgManagerId(organizationRegistration.getOrgManager().getId());
        } else {
            //get signed URL of file
            CompletableFuture<String> managerCidFrontFuture =
                    storageService.getSignedUrlAsync(organizationRegistration.getManagerCidFront());
            CompletableFuture<String> managerCidBackFuture =
                    storageService.getSignedUrlAsync(organizationRegistration.getManagerCidBack());
            CompletableFuture<String> managerCidHoldingFuture =
                    storageService.getSignedUrlAsync(organizationRegistration.getManagerCidHolding());

            List<CompletableFuture<String>> legalDocumentsFutures = new ArrayList<>();
            if(organizationRegistration.getLegalDocument() != null) {
                String[] legalDocuments = organizationRegistration.getLegalDocument().split("\\s+");
                List<String> legalDocumentsList = new ArrayList<>(Arrays.asList(legalDocuments));
                for (String legalDocument : legalDocumentsList) {
                    CompletableFuture<String> legalDocumentFuture =
                            storageService.getSignedUrlAsync(legalDocument);
                    legalDocumentsFutures.add(legalDocumentFuture);
                }
            }

            List<CompletableFuture<String>> otherEvidencesFutures = new ArrayList<>();
            if(organizationRegistration.getOtherEvidences() != null) {
                String[] otherEvidences = organizationRegistration.getOtherEvidences().split("\\s+");
                List<String> otherEvidencesList = new ArrayList<>(Arrays.asList(otherEvidences));
                for (String otherEvidence : otherEvidencesList) {
                    CompletableFuture<String> otherEvidenceFuture =
                            storageService.getSignedUrlAsync(otherEvidence);
                    otherEvidencesFutures.add(otherEvidenceFuture);
                }
            }

            String managerCidFrontUrl = null;
            String managerCidBackUrl = null;
            String managerCidHoldingUrl = null;
            List<String> legalDocumentsUrls = new ArrayList<>();
            List<String> otherEvidencesUrls = new ArrayList<>();

            try {
                CompletableFuture.allOf(managerCidFrontFuture, managerCidBackFuture, managerCidHoldingFuture).join();
                managerCidFrontUrl = managerCidFrontFuture.join();
                managerCidBackUrl = managerCidBackFuture.join();
                managerCidHoldingUrl = managerCidHoldingFuture.join();

                CompletableFuture.allOf(legalDocumentsFutures.toArray(new CompletableFuture[0])).join();

                CompletableFuture.allOf(otherEvidencesFutures.toArray(new CompletableFuture[0])).join();
                response.setManagerCidFrontUrl(managerCidFrontUrl);
                response.setManagerCidBackUrl(managerCidBackUrl);
                response.setManagerCidHoldingUrl(managerCidHoldingUrl);

                for (CompletableFuture<String> legalDocumentsFuture : legalDocumentsFutures) {
                    legalDocumentsUrls.add(legalDocumentsFuture.join());
                }
                response.setLegalDocumentsUrls(legalDocumentsUrls);

                for (CompletableFuture<String> otherEvidenceFuture : otherEvidencesFutures) {
                    otherEvidencesUrls.add(otherEvidenceFuture.join());
                }
                response.setOtherEvidencesUrls(otherEvidencesUrls);

            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof AppException ae) {
                    note.append(ae.getMessage()).append("\n");
                } else {
                    throw cause instanceof RuntimeException re ? re : e;
                }
            }
        }
        response.setNote(note.isEmpty() ? null : note.toString());
        return response;
    }

    @Override
    public void verifyOrgRegistration(UUID id, OrganizationRegistrationVerifyRequest request) {
        //get the registration from db
        OrganizationRegistration organizationRegistration = organizationRegistrationRepository.findById(id).orElseThrow(
                () -> new AppException(OrganizationErrorCode.REGISTRATION_NOT_EXISTED)
        );

        if (!organizationRegistration.getStatus().equals(EOrgRegistrationStatus.PENDING)) {
            throw new AppException(OrganizationErrorCode.REGISTRATION_VERIFIED);
        }

        //check the unique of email
        if (userRepository.existsByEmail(organizationRegistration.getManagerEmail())) {
            throw new AppException(OrganizationErrorCode.EMAIL_USED);
        }

        SystemAdmin currentAdmin = systemAdminRepository.getReferenceById(currentUserProvider.getId());
        organizationRegistration.setReviewedBy(currentAdmin);

        //delete cid images
        CompletableFuture<Void> f1 =
                storageService.deleteFileAsync(organizationRegistration.getManagerCidFront());
        CompletableFuture<Void> f2 =
                storageService.deleteFileAsync(organizationRegistration.getManagerCidBack());
        CompletableFuture<Void> f3 =
                storageService.deleteFileAsync(organizationRegistration.getManagerCidHolding());

        try {
            CompletableFuture.allOf(f1, f2, f3).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae && ae.getHttpStatus().value() == 400) {
                //todo: this case is the file not exist in sb (only for test) change later, need to have picture to approve
            } else {
                throw (RuntimeException) e.getCause(); // propagate, transaction fail
            }
        }

        organizationRegistration.setManagerCidFront("");
        organizationRegistration.setManagerCidBack("");
        organizationRegistration.setManagerCidHolding("");

        if (Boolean.TRUE.equals(request.getApprove())) {
            //APPROVE

            //Create organization in the db
            Organization organization = new Organization();
            organization.setName(organizationRegistration.getName());
            organization.setDhaRegistered(organizationRegistration.getDhaRegistered());
            organization.setOrgType(organizationRegistration.getOrgType());
            organization.setOrgIntroduction(organizationRegistration.getOrgIntroduction());
            organization.setLegalDocument(organizationRegistration.getLegalDocument());
            organization.setOtherEvidences(organizationRegistration.getOtherEvidences());
            organization.setStatus(EOrganizationStatus.ACTIVE);
            organization.setCreateBy(currentAdmin);

            organizationRepository.save(organization);

            //Create account in auth server
            String defaultPassword = RandomStringUtil.random8AlphaNumeric();
            UUID orgManagerId = authClient.createAccount(
                    ERole.ORG_MANAGER,
                    organizationRegistration.getManagerEmail(),
                    defaultPassword,
                    organizationRegistration.getManagerPhone()
            );

            //create organization manager in the db
            OrganizationManager organizationManager = new OrganizationManager();
            organizationManager.setId(orgManagerId);
            organizationManager.setCid(organizationRegistration.getManagerCid());
            organizationManager.setPhone(organizationRegistration.getManagerPhone());
            organizationManager.setEmail(organizationRegistration.getManagerEmail());
            organizationManager.setFullName(organizationRegistration.getManagerFullName());
            organizationManager.setOrganization(organization);
            organizationManager.setCreatedBy(currentAdmin);

            organizationManagerRepository.save(organizationManager);

            //update the organization registration record
            organizationRegistration.setStatus(EOrgRegistrationStatus.APPROVED);
            organizationRegistration.setReviewedBy(currentAdmin);
            organizationRegistration.setOrganization(organization);
            organizationRegistration.setOrgManager(organizationManager);

            organizationRegistrationRepository.save(organizationRegistration);

            emailService.sendApproveRegisterOrganizationEmail(organizationRegistration.getName(), organizationRegistration.getManagerEmail(), defaultPassword);

            log.info("Verify organization registration id={}, create organization manager account id={}", id, orgManagerId);
            return;
        }

        //REJECT
        //update the organization registration record
        organizationRegistration.setStatus(EOrgRegistrationStatus.REJECTED);
        organizationRegistration.setRejectionReason(request.getRejectionReason());
        organizationRegistrationRepository.save(organizationRegistration);
        //send email
        emailService.sendRejectRegisterOrganizationEmail(organizationRegistration.getManagerEmail(), request.getRejectionReason());
        log.info("Verify organization registration id={}, rejected", id);
    }

    @Override
    public Page<OrganizationSimpleResponse> getOrganizations(int pageNumber, int pageSize, String name, List<String> orgTypes) {

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "created_at")
        );

        List<Object[]> rawOrgData;

        //check if orgTypes is null or empty
        if(orgTypes == null || orgTypes.isEmpty()) {
            rawOrgData = organizationRepository.searchWithoutOrgType(name, pageable);
        } else {
            rawOrgData = organizationRepository.search(name, orgTypes, pageable);
        }
        List<OrganizationSimpleResponse> organizations = rawOrgData.stream()
                .map(organizationMapper::toOrganizationSimpleResponse).toList();

        return new PageImpl<>(organizations, pageable, organizations.size());
    }

    @Override
    public OrganizationDetailsResponseForSystemAdmin getOrganizationDetailsBySystemAdmin(UUID ordId) {
        //get the organization from db
        Organization organization = organizationRepository.findById(ordId).orElseThrow(
                () -> new AppException(OrganizationErrorCode.ORGANIZATION_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //get manager info

        UUID managerId = null;
        String managerName = null;
        String managerEmail = null;
        String managerPhone = null;
        String managerCID = null;

        OrganizationManager organizationManager = organizationManagerRepository.findByOrganizationId(ordId);

        if(organizationManager == null) {
            note.append(OrganizationErrorCode.NO_ORGANIZATION_MANAGER_FOUND.getMessage()).append("\n");
        } else {
            managerId = organizationManager.getId();
            managerName = organizationManager.getFullName();
            managerEmail = organizationManager.getEmail();
            managerPhone = organizationManager.getPhone();
            managerCID = organizationManager.getCid();
        }

        //get total of hosts
        Long totalHosts = hostRepository.countHostByOrganizationId(ordId);

        //get total honor hours
        Set<String> activitySubDomains = new HashSet<>();
        List<Event> events = eventRepository.findAllByOrganizationId(organization.getId());

        for(Event e : events) {
            activitySubDomains.add(e.getActivitySubDomain().getName());
        }

        //get signed urls
        CompletableFuture<String> avatarImageFuture = null;
        CompletableFuture<String> coverImageFuture = null;
        if(organization.getAvatarImage() != null && organization.getCoverImage() != null) {
            avatarImageFuture = storageService.getSignedUrlAsync(organization.getAvatarImage());
            coverImageFuture = storageService.getSignedUrlAsync(organization.getCoverImage());
        }

        List<CompletableFuture<String>> legalDocumentsFutures = new ArrayList<>();
        if(organization.getLegalDocument() != null) {
            String[] legalDocuments = organization.getLegalDocument().split("\\s+");
            List<String> legalDocumentsList = new ArrayList<>(Arrays.asList(legalDocuments));
            for (String legalDocument : legalDocumentsList) {
                CompletableFuture<String> legalDocumentFuture =
                        storageService.getSignedUrlAsync(legalDocument);
                legalDocumentsFutures.add(legalDocumentFuture);
            }
        }

        List<CompletableFuture<String>> otherEvidencesFutures = new ArrayList<>();
        if(organization.getOtherEvidences() != null) {
            String[] otherEvidences = organization.getOtherEvidences().split("\\s+");
            List<String> otherEvidencesList = new ArrayList<>(Arrays.asList(otherEvidences));
            for (String otherEvidence : otherEvidencesList) {
                CompletableFuture<String> otherEvidenceFuture =
                        storageService.getSignedUrlAsync(otherEvidence);
                otherEvidencesFutures.add(otherEvidenceFuture);
            }
        }

        List<String> legalDocumentsUrls = new ArrayList<>();
        List<String> otherEvidencesUrls = new ArrayList<>();
        String avatarImageUrl = null;
        String coverImageUrl = null;
        try {

            CompletableFuture.allOf(legalDocumentsFutures.toArray(new CompletableFuture[0])).join();

            CompletableFuture.allOf(otherEvidencesFutures.toArray(new CompletableFuture[0])).join();

            if(avatarImageFuture != null && coverImageFuture != null) {
                CompletableFuture.allOf(avatarImageFuture, coverImageFuture).join();
                avatarImageUrl = avatarImageFuture.join();
                coverImageUrl = coverImageFuture.join();
            }

            for (CompletableFuture<String> legalDocumentsFuture : legalDocumentsFutures) {
                legalDocumentsUrls.add(legalDocumentsFuture.join());
            }

            for (CompletableFuture<String> otherEvidenceFuture : otherEvidencesFutures) {
                otherEvidencesUrls.add(otherEvidenceFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                note.append(ae.getMessage()).append("\n");
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        return OrganizationDetailsResponseForSystemAdmin.builder()
                .id(organization.getId())
                .name(organization.getName())
                .dhaRegistered(organization.getDhaRegistered())
                .orgType(organization.getOrgType())
                .orgIntroduction(organization.getOrgIntroduction())
                .createdAt(organization.getCreatedAt())
                .avatarImageUrl(avatarImageUrl)
                .coverImageUrl(coverImageUrl)
                .legalDocumentUrls(legalDocumentsUrls)
                .otherEvidencesUrls(otherEvidencesUrls)
                .managerId(managerId)
                .managerName(managerName)
                .managerEmail(managerEmail)
                .managerPhone(managerPhone)
                .managerCID(managerCID)
                .totalHosts(totalHosts)
                .hostedEventCount(organization.getHostedEventCount())
                .creditHour(organization.getCreditHour())
                .avgRating(organization.getAvgRating())
                .status(organization.getStatus())
                .activitySubDomains(activitySubDomains)
                .note(note.toString())
                .build();
    }

    @Override
    public OrganizationDetailsResponse getOrganizationDetails(UUID ordId) {
        //get the organization from db
        Organization organization = organizationRepository.findById(ordId).orElseThrow(
                () -> new AppException(OrganizationErrorCode.ORGANIZATION_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //get manager info
        UUID managerId = null;
        String managerEmail = null;
        String managerPhone = null;

        OrganizationManager organizationManager = organizationManagerRepository.findByOrganizationId(ordId);

        if(organizationManager == null) {
            note.append(OrganizationErrorCode.NO_ORGANIZATION_MANAGER_FOUND.getMessage()).append("\n");
        } else {
            managerId = organizationManager.getId();
            managerEmail = organizationManager.getEmail();
            managerPhone = organizationManager.getPhone();
        }

        //get total honor hours
        long totalHonorHours = 0;
        List<Event> events = eventRepository.findAllByOrganizationId(ordId);

        for(Event e : events) {

            for(EventSession es : e.getSessions()) {
                totalHonorHours += Duration.between(es.getStartDateTime(), es.getEndDateTime()).toHours();
            }
        }

        //get signed urls
        CompletableFuture<String> avatarImageFuture = null;
        CompletableFuture<String> coverImageFuture = null;
        if(organization.getAvatarImage() != null && organization.getCoverImage() != null) {
            avatarImageFuture = storageService.getSignedUrlAsync(organization.getAvatarImage());
            coverImageFuture = storageService.getSignedUrlAsync(organization.getCoverImage());
        }

        String avatarImageUrl = null;
        String coverImageUrl = null;
        try {
            if(avatarImageFuture != null && coverImageFuture != null) {
                CompletableFuture.allOf(avatarImageFuture, coverImageFuture).join();
                avatarImageUrl = avatarImageFuture.join();
                coverImageUrl = coverImageFuture.join();
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                note.append(ae.getMessage()).append("\n");
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        return OrganizationDetailsResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .dhaRegistered(organization.getDhaRegistered())
                .orgType(organization.getOrgType())
                .orgIntroduction(organization.getOrgIntroduction())
                .createdAt(organization.getCreatedAt())
                .avatarImageUrl(avatarImageUrl)
                .coverImageUrl(coverImageUrl)
                .managerId(managerId)
                .managerEmail(managerEmail)
                .managerPhone(managerPhone)
                .totalHonorHours(totalHonorHours)
                .note(note.toString())
                .build();
    }

    @Override
    public void deductCreditHourOfOrganization(Organization organization, int numberOfHourDeduct) {
        organization.setCreditHour(organization.getCreditHour()- numberOfHourDeduct);
        organizationRepository.save(organization);
        log.info("The credit hour of organization was deducted by 3, organizationId={}", organization.getId());
    }

    @Override
    @Transactional
    public void calculateOrganizationsAvgRating() {
        //get events that end for 7 days
        LocalDate targetDate = LocalDate.now().minusDays(7);
        List<EventOrganizationProjection> projections = eventRepository.findCompletedEventsAndEndDateAt(targetDate);

        Map<Organization, List<Event>> map = new HashMap<>();

        // group by organization
        for (EventOrganizationProjection p : projections) {
            map.computeIfAbsent(p.getOrganization(), k -> new ArrayList<>())
                    .add(p.getEvent());
        }

        //recalculate avg rating for each organization
        for (Map.Entry<Organization, List<Event>> entry : map.entrySet()) {
            Organization org = entry.getKey();
            List<Event> events = entry.getValue();

            int totalRating = org.getAvgRating() * org.getHostedEventCount();
            int totalCount = org.getHostedEventCount();

            for (Event e : events) {
                totalRating += e.getAvgRating();
                totalCount++;
            }

            org.setAvgRating((short) (totalRating / totalCount));
            org.setHostedEventCount(totalCount);
            log.info("Updated avg rating for organization, organizationId={}", org.getId());
        }

        organizationRepository.saveAll(map.keySet());
        log.info("Updated avg rating for {} organizations", map.size());
    }

    @Override
    @Transactional
    //todo delete this
    public void calculateOrganizationsAvgRatingForMockData() {
        //get events that end for 7 days
        LocalDate targetDate = LocalDate.now().minusDays(7);
        List<EventOrganizationProjection> projections = eventRepository.findCompletedEventsAndEndDateBefore(targetDate);

        Map<Organization, List<Event>> map = new HashMap<>();

        // group by organization
        for (EventOrganizationProjection p : projections) {
            map.computeIfAbsent(p.getOrganization(), k -> new ArrayList<>())
                    .add(p.getEvent());
        }

        //recalculate avg rating for each organization
        for (Map.Entry<Organization, List<Event>> entry : map.entrySet()) {
            Organization org = entry.getKey();
            List<Event> events = entry.getValue();

            int totalRating = org.getAvgRating() * org.getHostedEventCount();
            int totalCount = org.getHostedEventCount();

            for (Event e : events) {
                totalRating += e.getAvgRating();
                totalCount++;
            }

            org.setAvgRating((short) (totalRating / totalCount));
            org.setHostedEventCount(totalCount);
            log.info("Updated avg rating for organization, organizationId={}", org.getId());
        }

        organizationRepository.saveAll(map.keySet());
        log.info("Updated avg rating for {} organizations", map.size());
    }

    @Override
    public Page<OrganizationSimpleResponseForSystemAdmin> getOrganizationsBySystemAdmin(int pageNumber, int pageSize, String name, List<String> orgTypes) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Organization> rawOrgData;

        //check if orgTypes is null or empty
        if(orgTypes == null || orgTypes.isEmpty()) {
            rawOrgData = organizationRepository.searchByAdminWithoutOrgType(name, pageable);
        } else {
            rawOrgData = organizationRepository.searchByAdmin(name, orgTypes, pageable);
        }

        return rawOrgData
                .map(o -> {
                    Set<String> activitySubDomains = new HashSet<>();
                    List<Event> events = eventRepository.findAllByOrganizationId(o.getId());

                    for(Event e : events) {
                        activitySubDomains.add(e.getActivitySubDomain().getName());
                    }

                    return new OrganizationSimpleResponseForSystemAdmin(
                            o.getId(),
                            o.getName(),
                            o.getOrgType(),
                            o.getHostedEventCount(),
                            o.getCreditHour(),
                            o.getAvgRating(),
                            o.getStatus(),
                            activitySubDomains
                    );
                });
    }
}
