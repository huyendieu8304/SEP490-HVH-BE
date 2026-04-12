package com.sep490.g28.hvh.be.service.impl;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.ERole;
import com.sep490.g28.hvh.be.constant.EVolunteerVerificationStatus;
import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.VolunteerRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.volunteer.response.*;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.FaceApiErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.faceServer.FaceClient;
import com.sep490.g28.hvh.be.integration.faceServer.dto.FaceRegisterResponse;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.VolunteerService;
import com.sep490.g28.hvh.be.util.RandomStringUtil;
import com.sep490.g28.hvh.be.util.StringNormalizeUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static com.sep490.g28.hvh.be.util.StringNormalizeUtil.normalizeVietnameseName;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VolunteerServiceImpl implements VolunteerService {

    VolunteerRepository volunteerRepository;
    UserRepository userRepository;
    IdentityVerificationRepository identityVerificationRepository;
    CertificateRepository certificateRepository;
    StorageService storageService;
    StoragePathGenerator storagePathGenerator;
    OtpService otpService;
    AuthClient authClient;
    FaceClient faceClient;
    SystemAdminRepository systemAdminRepository;
    CurrentUserProvider currentUserProvider;
    EmailService emailService;

    @Override
    public RegisterVolunteerAccountResponse registerVolAccount (RegisterVolunteerAccountRequest request) {

        //1. validate otp
        otpService.verifyVolAccountRegistrationOtp(request.getEmail(), request.getOtp());

        //2. check the unique email, cid, phone in the volunteers account
        checkUniqueEmailCidPhone(request.getEmail(), request.getCid(), request.getPhone());

        IdentityVerification verification = new IdentityVerification();
        UUID id = UUID.randomUUID();
        verification.setId(id);
        verification.setStatus(EVolunteerVerificationStatus.PENDING);

        //3. generate upload url for fe
        //get the path in storage
        String cidFrontPath = storagePathGenerator.identityVerificationCidFront(id, request.getCidFrontFileExtension());
        String cidBackPath = storagePathGenerator.identityVerificationCidBack(id, request.getCidBackFileExtension());
        String cidHoldingPath = storagePathGenerator.identityVerificationCidHolding(id, request.getCidHoldingFileExtension());
        //get upload url
        CompletableFuture<String> cidFrontFuture =
                storageService.getUploadUrlAsync(cidFrontPath);
        CompletableFuture<String> cidBackFuture =
                storageService.getUploadUrlAsync(cidBackPath);
        CompletableFuture<String> cidHoldingFuture =
                storageService.getUploadUrlAsync(cidHoldingPath);

        try {
            CompletableFuture.allOf(cidFrontFuture, cidBackFuture, cidHoldingFuture).join();
        } catch (CompletionException e) {
            throw (RuntimeException) e.getCause();
        }

        String cidFrontUploadUrl = cidFrontFuture.join();
        String cidBackUploadUrl = cidBackFuture.join();
        String cidHoldingUploadUrl = cidHoldingFuture.join();

        //4. create volunteer verification request in db
        verification.setCid(request.getCid());
        verification.setEmail(request.getEmail());
        verification.setPhone(request.getPhone());
        verification.setFullName(StringNormalizeUtil.normalizeVietnameseName(request.getFullName()));

        verification.setCidFront(cidFrontPath);
        verification.setCidBack(cidBackPath);
        verification.setCidHolding(cidHoldingPath);

        identityVerificationRepository.save(verification);
        log.info("Create new identity verification: {}", verification.getId());

        return RegisterVolunteerAccountResponse.builder()
                .cidBackUploadUrl(cidBackUploadUrl)
                .cidFrontUploadUrl(cidFrontUploadUrl)
                .cidHoldingUploadUr(cidHoldingUploadUrl)
                .build();
    }

    private void checkUniqueEmailCidPhone(String email, String cid, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new AppException(VolunteerErrorCode.EMAIL_USED);
        }
        if (volunteerRepository.existsByCid(cid)) {
            throw new AppException(VolunteerErrorCode.CID_USED);
        }
        if (volunteerRepository.existsByPhone(phone)) {
            throw new AppException(VolunteerErrorCode.PHONE_USED);
        }
    }

    @Override
    public Page<VolunteerRegistrationSimpleResponse> getVolRegistrations(int pageNumber, int pageSize, String inputStatus, String email) {
        //parse status
        EVolunteerVerificationStatus status =
                (inputStatus == null || inputStatus.isBlank())
                        ? null
                        : EVolunteerVerificationStatus.valueOf(inputStatus);

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        //search
        return identityVerificationRepository.search(status, email, pageable)
                .map(VolunteerRegistrationSimpleResponse::from);
    }

    @Override
    public VolunteerRegistrationDetailsResponse getVolRegistrationDetails(UUID id) {
        //check id exist
        IdentityVerification identityVerification = identityVerificationRepository.findById(id).orElseThrow(
                () -> new AppException(VolunteerErrorCode.REGISTRATION_NOT_EXISTED)
        );

        StringBuilder note = new StringBuilder();

        //check whether email used by any account
        if (userRepository.existsByEmail(identityVerification.getEmail())) {
            //add to the note to announce sys_admin
            note.append(VolunteerErrorCode.EMAIL_USED.getMessage()).append("\n");
        }
        //check whether cid used by any volunteer
        if (volunteerRepository.existsByCid(identityVerification.getCid())) {
            note.append(VolunteerErrorCode.CID_USED.getMessage()).append("\n");
        }
        //check whether phone used by any volunteer
        if (volunteerRepository.existsByPhone(identityVerification.getPhone())) {
            note.append(VolunteerErrorCode.PHONE_USED.getMessage()).append("\n");
        }

        //build response
        VolunteerRegistrationDetailsResponse response = VolunteerRegistrationDetailsResponse.builder()
                .id(identityVerification.getId())
                .cid(identityVerification.getCid())
                .email(identityVerification.getEmail())
                .phone(identityVerification.getPhone())
                .status(identityVerification.getStatus())
                .rejectionReason(identityVerification.getRejectionReason())
                .createdAt(identityVerification.getCreatedAt())
                .reviewAt(identityVerification.getReviewedAt())
                .build();
        if (identityVerification.getStatus() != EVolunteerVerificationStatus.PENDING) {
            response.setAdminId(identityVerification.getReviewedBy().getId().toString());
            response.setAdminEmail(identityVerification.getReviewedBy().getEmail());
            response.setVolunteerId(identityVerification.getVolunteer().getVid().toString());
            response.setVolunteerEmail(identityVerification.getVolunteer().getEmail());
        } else {
            //get signed URL of file
            CompletableFuture<String> cidFrontFuture = storageService.getSignedUrlAsync(identityVerification.getCidFront());
            CompletableFuture<String> cidBackFuture = storageService.getSignedUrlAsync(identityVerification.getCidBack());
            CompletableFuture<String> cidHoldingFuture = storageService.getSignedUrlAsync(identityVerification.getCidHolding());

            try {
                CompletableFuture.allOf(cidFrontFuture, cidBackFuture, cidHoldingFuture).join();
                response.setCidFrontUrl(cidFrontFuture.join());
                response.setCidBackUrl(cidBackFuture.join());
                response.setCidHoldingUrl(cidHoldingFuture.join());
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
    public void verifyVolRegistration(UUID id, VolunteerRegistrationVerifyRequest request) {
        //get the registration from db
        IdentityVerification identityVerification = identityVerificationRepository.findById(id).orElseThrow(
                () -> new AppException(VolunteerErrorCode.REGISTRATION_NOT_EXISTED)
        );

        //the registration is already verified
        if (!identityVerification.getStatus().equals(EVolunteerVerificationStatus.PENDING)){
            throw new AppException(VolunteerErrorCode.REGISTRATION_VERIFIED);
        }

        //get the current admin who make the request
        SystemAdmin currentAdmin = systemAdminRepository.getReferenceById(currentUserProvider.getId());
        identityVerification.setReviewedBy(currentAdmin);

        if (Boolean.TRUE.equals(request.getApprove())){
            //APPROVE
            //check the unique of email, cid, phone
            checkUniqueEmailCidPhone(identityVerification.getEmail(), identityVerification.getCid(), identityVerification.getPhone());

            //Create account in auth server
            String defaultPassword = RandomStringUtil.random8AlphaNumeric();
            UUID volunteerId = authClient.createAccount(
                    ERole.VOL,
                    identityVerification.getEmail(),
                    defaultPassword,
                    identityVerification.getPhone()
            );

            //create volunteer in the db
            Volunteer volunteer = new Volunteer();
            volunteer.setId(volunteerId);
            volunteer.setVid(UUID.randomUUID());
            volunteer.setCid(identityVerification.getCid());
            volunteer.setEmail(identityVerification.getEmail());
            volunteer.setPhone(identityVerification.getPhone());
            if (request.getFullName() != null ) {
                volunteer.setFullName(normalizeVietnameseName(request.getFullName()));
            }
            volunteer.setCreatedBy(currentAdmin);

            volunteerRepository.save(volunteer);

            //update the identity verification record
            identityVerification.setStatus(EVolunteerVerificationStatus.APPROVED);
            identityVerification.setReviewedBy(currentAdmin);
            identityVerification.setVolunteer(volunteer);

            //send mail to the volunteer
            emailService.sendApproveRegisterVolAccountEmail(identityVerification.getEmail(), defaultPassword);
            log.info("Verify identity id={}, create volunteer account id={}", id, volunteerId);
        } else {
            //REJECT
            //update the identity verification record
            identityVerification.setStatus(EVolunteerVerificationStatus.REJECTED);
            identityVerification.setRejectionReason(request.getRejectionReason());
            //send mail
            emailService.sendRejectRegisterVolAccountEmail(identityVerification.getEmail(), request.getRejectionReason());
            log.info("Verify identity id={}, rejected", id);
        }

        //delete cid images
        CompletableFuture<Void> f1 =
                storageService.deleteFileAsync(identityVerification.getCidFront());
        CompletableFuture<Void> f2 =
                storageService.deleteFileAsync(identityVerification.getCidBack());
        CompletableFuture<Void> f3 =
                storageService.deleteFileAsync(identityVerification.getCidHolding());
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
        identityVerification.setCidFront("");
        identityVerification.setCidBack("");
        identityVerification.setCidHolding("");

        //update identity verification request
        identityVerificationRepository.save(identityVerification);
    }

    //todo unit test
    @Override
    public Page<VolunteerSimpleResponseForAdmin> getVolunteersByAdmin(int pageNumber, int pageSize, String email) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        Page<VolunteerSimpleResponseForAdmin> page =
                volunteerRepository.findVolunteersByAdmin(pageable, email);

        //get avatar signed urls
        List<CompletableFuture<VolunteerSimpleResponseForAdmin>> futures =
                page.getContent().stream()
                        .map(v -> {
                            if (v.getAvatarUrl() == null) {
                                return CompletableFuture.completedFuture(v);
                            }
                            return storageService.getSignedUrlAsync(v.getAvatarUrl())
                                    .thenApply(url -> {
                                        v.setAvatarUrl(url);
                                        return v;
                                    })
                                    //todo this might be put into some todos
                                    .exceptionally(ex -> {
                                        log.warn("Failed to get signed url for path: {}", v.getAvatarUrl(), ex);
                                        v.setAvatarUrl(null);
                                        return v;
                                    });
                        })
                        .toList();

        List<VolunteerSimpleResponseForAdmin> content =
                futures.stream().map(CompletableFuture::join).toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());

    }

    @Override
    public Page<VolunteerActivitiesResponseForAdmin> getVolunteerActivitiesByAdmin(
            UUID volunteerId,
            int pageNumber,
            int pageSize
    ) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        return volunteerRepository.getVolunteerActivitiesByAdmin(pageable, volunteerId);
    }

    @Override
    public VolunteerPublicInformationResponse getVolunteerPublicInformation(UUID volunteerId) {

        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new AppException(VolunteerErrorCode.VOLUNTEER_NOT_EXISTED));



        //get certificates of volunteer
        List<Certificate> certificateList = certificateRepository.findByVolunteerId(volunteerId);

        //get signed URL of certificates and volunteer's avatar
        String avatarUrl = null;
        CompletableFuture<String> avatarFuture = null;
        //check if volunteer has avatar
        if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {
            avatarFuture = storageService.getSignedUrlAsync(volunteer.getAvatarUrl());
        }

        List<CompletableFuture<String>> certificatesFutures = new ArrayList<>();
        if (certificateList != null && !certificateList.isEmpty()) {
            for (Certificate certificate : certificateList) {
                CompletableFuture<String> certificateFuture =
                        storageService.getSignedUrlAsync(certificate.getCertificatePath());
                certificatesFutures.add(certificateFuture);
            }
        }

        List<String> certificatesUrls = new ArrayList<>();
        try {
            if (avatarFuture != null) {
                CompletableFuture.allOf(avatarFuture).join();
                avatarUrl = avatarFuture.join();
            }

            CompletableFuture.allOf(certificatesFutures.toArray(new CompletableFuture[0])).join();
            for (CompletableFuture<String> certificateFuture : certificatesFutures) {
                certificatesUrls.add(certificateFuture.join());
            }

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AppException ae) {
                //todo: handle exception at getEventDetails
            } else {
                throw cause instanceof RuntimeException re ? re : e;
            }
        }

        return new VolunteerPublicInformationResponse(
                volunteer.getVid(),
                volunteer.getFullName(),
                volunteer.getNickname(),
                volunteer.getBio(),
                volunteer.getDob(),
                avatarUrl,
                volunteer.getCreditScore(),
                volunteer.getAvgRating(),
                volunteer.getActivityCount(),
                certificatesUrls
        );
    }

    @Override
    public VolunteerAccountInformationResponse getVolunteerAccountInformation() {

        UUID volunteerId = currentUserProvider.getId();

        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new AppException(VolunteerErrorCode.VOLUNTEER_NOT_EXISTED));

        //get signed URL of volunteer avatar
        String avatarUrl = null;
        if (volunteer.getAvatarUrl() != null && !volunteer.getAvatarUrl().isEmpty()) {

            CompletableFuture<String> avatarFuture =
                    storageService.getSignedUrlAsync(volunteer.getAvatarUrl());

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

        return new VolunteerAccountInformationResponse(
                volunteerId,
                volunteer.getVid(),
                volunteer.getCid(),
                volunteer.getEmail(),
                volunteer.getPhone(),
                volunteer.isPhoneVerified(),
                volunteer.getNickname(),
                volunteer.getFullName(),
                volunteer.getBio(),
                volunteer.isGender(),
                volunteer.getDob(),
                volunteer.getLevel(),
                avatarUrl,
                volunteer.getAddress(),
                volunteer.getDetailAddress(),
                volunteer.getEmployStatus(),
                volunteer.getWorkAddress(),
                volunteer.getEducationLevel(),
                volunteer.getSid(),
                volunteer.getCreditScore(),
                volunteer.getHonorScore(),
                volunteer.getAvgRating(),
                volunteer.getActivityCount()
        );
    }

    @Override
    public void registerVolunteerFace(MultipartFile file) {
        UUID volunteerId = currentUserProvider.getId();

        User user = userRepository.findById(volunteerId)
                .orElseThrow(() -> new AppException(AppCommonErrorCode.ACCOUNT_NOT_EXISTED));

        if(user.isFaceRegistered()) {
           throw new AppException(FaceApiErrorCode.ALREADY_REGISTERED_FACE);
        }

        Volunteer volunteer = volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> new AppException(VolunteerErrorCode.VOLUNTEER_NOT_EXISTED));

        FaceRegisterResponse response = faceClient
                .faceRegister(convertToValidUsername(volunteer.getFullName()), volunteerId, file);

        if(response.success()) {
            user.setFaceRegistered(true);
            userRepository.save(user);
        }
    }

    private String convertToValidUsername(String str) {
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        return temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("đ", "d")
                .replaceAll("Đ", "D")
                .replaceAll("\\s+", "");
    }
}
