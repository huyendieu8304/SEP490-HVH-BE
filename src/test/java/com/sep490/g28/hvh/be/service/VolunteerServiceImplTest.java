package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EEducationLevel;
import com.sep490.g28.hvh.be.constant.EEmployStatus;
import com.sep490.g28.hvh.be.constant.EVolunteerVerificationStatus;
import com.sep490.g28.hvh.be.dto.volunteer.request.*;
import com.sep490.g28.hvh.be.dto.volunteer.response.*;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.faceServer.FaceAuthClient;
import com.sep490.g28.hvh.be.integration.faceServer.dto.RegisterFaceResponse;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.VolunteerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VolunteerServiceImplTest {

    @Mock
    VolunteerRepository volunteerRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    IdentityVerificationRepository identityVerificationRepository;
    @Mock
    StorageService storageService;
    @Mock
    StoragePathGenerator storagePathGenerator;
    @Mock
    OtpService otpService;

//    @InjectMocks
    VolunteerServiceImpl volunteerService;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    SystemAdminRepository systemAdminRepository;

    @Mock
    AuthClient authClient;

    @Mock
    FaceAuthClient faceAuthClient;

    @Mock
    EmailService emailService;

    @Mock
    CertificateRepository certificateRepository;

    UUID id;
    UUID volunteerId;
    SystemAdmin admin;

    @BeforeEach
    void setup() {
        //ínsteaed of inectMocks
        volunteerService = new VolunteerServiceImpl(
                volunteerRepository,
                userRepository,
                identityVerificationRepository,
                certificateRepository,
                storageService,
                storagePathGenerator,
                otpService,
                authClient,
                faceAuthClient,
                systemAdminRepository,
                currentUserProvider,
                emailService
        );
        id = UUID.randomUUID();
        volunteerId = UUID.randomUUID();
//        admin = new SystemAdmin();
//        admin.setId(UUID.randomUUID());
//
//        when(currentUserProvider.getId()).thenReturn(admin.getId());
//        when(systemAdminRepository.getReferenceById(any())).thenReturn(admin);
//
//        when(storageService.deleteFileAsync(any()))
//                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private RegisterVolunteerAccountRequest validRegisterVolunteerAccountRequest() {
        RegisterVolunteerAccountRequest req = new RegisterVolunteerAccountRequest();
        req.setOtp("123456");
        req.setEmail("nguyenvanA@gmail.com");
        req.setPhone("0916234940");
        req.setCid("034309880903");
        req.setCidFrontFileExtension(".jpeg");
        req.setCidBackFileExtension(".png");
        req.setCidHoldingFileExtension(".jpg");
        return req;
    }

    private IdentityVerification validIdentityVerification() {
        IdentityVerification identityVerification = new IdentityVerification();
        identityVerification.setId(UUID.randomUUID());
        identityVerification.setEmail("vol@mail.com");
        identityVerification.setCid("123456");
        identityVerification.setPhone("0909");
        identityVerification.setStatus(EVolunteerVerificationStatus.PENDING);
        identityVerification.setCidFront("f1");
        identityVerification.setCidBack("f2");
        identityVerification.setCidHolding("f3");
        return identityVerification;
    }

    private VolunteerRegistrationVerifyRequest approveRequest() {
        VolunteerRegistrationVerifyRequest req = new VolunteerRegistrationVerifyRequest();
        req.setApprove(true);
        req.setFullName("Nguyen Van A");
        return req;
    }

    private VolunteerRegistrationVerifyRequest rejectRequest() {
        VolunteerRegistrationVerifyRequest req = new VolunteerRegistrationVerifyRequest();
        req.setApprove(false);
        req.setRejectionReason("invalid info");
        return req;
    }

    private VolunteerSimpleResponseForAdmin mockVolunteer(String avatarUrl) {
        VolunteerSimpleResponseForAdmin v = new VolunteerSimpleResponseForAdmin();
        v.setAvatarUrl(avatarUrl);
        return v;
    }


    private UpdateVolunteerProfileRequest validUpdateRequest() {
        UpdateVolunteerProfileRequest u = new UpdateVolunteerProfileRequest();
        u.setFullName("Nguyen Van A");
        return u;
    }

    private UpdateVolunteerProfileBySystemAdminRequest validAdminRequest() {
        UpdateVolunteerProfileBySystemAdminRequest u = new UpdateVolunteerProfileBySystemAdminRequest();
        u.setFullName("Nguyen Van A");
        return u;
    }

    // ==== registerVolAccount ===================================
    // ==== TC01
    @Test
    void register_success() {
        RegisterVolunteerAccountRequest request = validRegisterVolunteerAccountRequest();
        when(otpService.verifyVolAccountRegistrationOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(volunteerRepository.existsByCid(any())).thenReturn(false);
        when(volunteerRepository.existsByPhone(any())).thenReturn(false);

        when(storagePathGenerator.identityVerificationCidFront(any(), any())).thenReturn("front-path");
        when(storagePathGenerator.identityVerificationCidBack(any(), any())).thenReturn("back-path");
        when(storagePathGenerator.identityVerificationCidHolding(any(), any())).thenReturn("holding-path");

        when(storageService.getUploadUrlAsync("front-path"))
                .thenReturn(CompletableFuture.completedFuture("front-url"));
        when(storageService.getUploadUrlAsync("back-path"))
                .thenReturn(CompletableFuture.completedFuture("back-url"));
        when(storageService.getUploadUrlAsync("holding-path"))
                .thenReturn(CompletableFuture.completedFuture("holding-url"));

        RegisterVolunteerAccountResponse response =
                volunteerService.registerVolAccount(request);

        verify(identityVerificationRepository).save(any(IdentityVerification.class));

        assertEquals("front-url", response.getCidFrontUploadUrl());
        assertEquals("back-url", response.getCidBackUploadUrl());
        assertEquals("holding-url", response.getCidHoldingUploadUr());
    }

    // ===== TC2 =====
    @Test
    void register_fail_invalid_otp() {
        doThrow(new AppException(AppCommonErrorCode.OTP_INVALID))
                .when(otpService)
                .verifyVolAccountRegistrationOtp(any(), any());

        AppException ex = assertThrows(
                AppException.class,
                () -> volunteerService.registerVolAccount(validRegisterVolunteerAccountRequest())
        );
        assertEquals(AppCommonErrorCode.OTP_INVALID.getCode(), ex.getCode());

        verify(identityVerificationRepository, never()).save(any());
    }

    // ===== TC3 =====
    @Test
    void register_fail_cid_used() {
        RegisterVolunteerAccountRequest request = validRegisterVolunteerAccountRequest();
        when(otpService.verifyVolAccountRegistrationOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(volunteerRepository.existsByCid(any())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> volunteerService.registerVolAccount(request)
        );
        assertEquals(VolunteerErrorCode.CID_USED.getCode(), ex.getCode());
        verify(identityVerificationRepository, never()).save(any());

    }

    // ===== TC4 =====
    @Test
    void register_fail_email_used() {
        RegisterVolunteerAccountRequest request = validRegisterVolunteerAccountRequest();
        when(otpService.verifyVolAccountRegistrationOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> volunteerService.registerVolAccount(request)
        );
        assertEquals(VolunteerErrorCode.EMAIL_USED.getCode(), ex.getCode());
        verify(identityVerificationRepository, never()).save(any());

    }

    // ===== TC5 =====
    @Test
    void register_fail_phone_used() {
        RegisterVolunteerAccountRequest request = validRegisterVolunteerAccountRequest();
        when(otpService.verifyVolAccountRegistrationOtp(request.getEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(volunteerRepository.existsByCid(any())).thenReturn(false);
        when(volunteerRepository.existsByPhone(any())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> volunteerService.registerVolAccount(request)
        );
        assertEquals(VolunteerErrorCode.PHONE_USED.getCode(), ex.getCode());
        verify(identityVerificationRepository, never()).save(any());
    }

    // ==== getVolRegistrations ======================================
    // ===== TC1 =====
    @Test
    void getVolRegistrations_should_return_page_with_status_and_email() {
        int pageNumber = 0;
        int pageSize = 10;
        String statusInput = "PENDING";
        String email = "nguyenvanA@gmail.com";

        IdentityVerification iv = new IdentityVerification();
        iv.setStatus(EVolunteerVerificationStatus.PENDING);

        Page<IdentityVerification> mockPage =
                new PageImpl<>(List.of(iv));

        when(identityVerificationRepository.search(
                eq(EVolunteerVerificationStatus.PENDING),
                eq(email),
                any(Pageable.class)
        )).thenReturn(mockPage);

        Page<VolunteerRegistrationSimpleResponse> result =
                volunteerService.getVolRegistrations(pageNumber, pageSize, statusInput, email);

        assertEquals(1, result.getTotalElements());

        verify(identityVerificationRepository)
                .search(eq(EVolunteerVerificationStatus.PENDING),
                        eq(email),
                        any(Pageable.class));
    }

    // ===== TC2 =====
    @Test
    void getVolRegistrations_should_pass_null_status_when_input_blank() {
        int pageNumber = 0;
        int pageSize = 10;

        Page<IdentityVerification> mockPage =
                new PageImpl<>(List.of(new IdentityVerification()));

        when(identityVerificationRepository.search(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(mockPage);

        Page<VolunteerRegistrationSimpleResponse> result =
                volunteerService.getVolRegistrations(pageNumber, pageSize, "   ", null);

        assertEquals(1, result.getTotalElements());

        verify(identityVerificationRepository)
                .search(isNull(), isNull(), any(Pageable.class));
    }

    // ===== getVolRegistrationDetails ============================================
    // ===== TC1 =====
    @Test
    void getVolRegistrationDetails_success() {
        UUID id = UUID.randomUUID();

        IdentityVerification iv = new IdentityVerification();
        iv.setId(id);
        iv.setEmail("nguyenvanA@gmail.com");
        iv.setCid("034309880903");
        iv.setPhone("0916234940");
        iv.setStatus(EVolunteerVerificationStatus.PENDING);

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(iv));

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(volunteerRepository.existsByCid(any())).thenReturn(false);
        when(volunteerRepository.existsByPhone(any())).thenReturn(false);

        VolunteerRegistrationDetailsResponse res =
                volunteerService.getVolRegistrationDetails(id);

        assertEquals("url", res.getCidFrontUrl());
        assertNull(res.getNote());
    }

    // ===== TC2 =====
    @Test
    void getVolRegistrationDetails_idNotExist_shouldThrowException() {
        UUID invalidId = UUID.randomUUID();
        when(identityVerificationRepository.findById(any())).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> volunteerService.getVolRegistrationDetails(invalidId));

        assertEquals(VolunteerErrorCode.REGISTRATION_NOT_EXISTED.getCode(), ex.getCode());
    }

//    // =====  =====
//    @Test
//    void getVolRegistrationDetails_pending_signedUrlSuccess() {
//
//        IdentityVerification identityVerification = validIdentityVerification();
//        when(identityVerificationRepository.findById(id))
//                .thenReturn(Optional.of(identityVerification));
//
//        when(storageService.getSignedUrlAsync(any()))
//                .thenReturn(CompletableFuture.completedFuture("signed-url"));
//
//        VolunteerRegistrationDetailsResponse res =
//                volunteerService.getVolRegistrationDetails(id);
//
//        assertEquals("signed-url", res.getCidFrontUrl());
//        assertNull(res.getAdminEmail());
//    }

    // ===== TC3 =====
    @Test
    void getVolRegistrationDetails_emailCidPhoneUsed_shouldAppendNote() {

        IdentityVerification identityVerification = validIdentityVerification();
        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        when(userRepository.existsByEmail(any())).thenReturn(true);
        when(volunteerRepository.existsByCid(any())).thenReturn(true);
        when(volunteerRepository.existsByPhone(any())).thenReturn(true);

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        VolunteerRegistrationDetailsResponse res =
                volunteerService.getVolRegistrationDetails(id);

        assertTrue(res.getNote().contains(
                VolunteerErrorCode.EMAIL_USED.getMessage()));
        assertTrue(res.getNote().contains(
                VolunteerErrorCode.CID_USED.getMessage()));
        assertTrue(res.getNote().contains(
                VolunteerErrorCode.PHONE_USED.getMessage()));
    }

    // ===== TC4 =====
    @Test
    void getVolRegistrationDetails_statusNotPending_shouldReturnAdminVolunteerInfo() {

        IdentityVerification identityVerification = new IdentityVerification();
        identityVerification.setStatus(EVolunteerVerificationStatus.APPROVED);

        SystemAdmin admin = new SystemAdmin();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@mail.com");

        Volunteer volunteer = new Volunteer();
        volunteer.setVid(UUID.randomUUID());
        volunteer.setEmail("vol@mail.com");

        identityVerification.setReviewedBy(admin);
        identityVerification.setVolunteer(volunteer);

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        VolunteerRegistrationDetailsResponse res =
                volunteerService.getVolRegistrationDetails(id);

        assertEquals("admin@mail.com", res.getAdminEmail());
        assertEquals("vol@mail.com", res.getVolunteerEmail());
    }

    // ===== TC5 =====
    @Test
    void getVolRegistrationDetails_signedUrlFail_shouldSetNote() {

        IdentityVerification identityVerification = validIdentityVerification();
        when(identityVerificationRepository.findById(any()))
                .thenReturn(Optional.of(identityVerification));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)
        );

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(failedFuture);

        VolunteerRegistrationDetailsResponse res =
                volunteerService.getVolRegistrationDetails(id);

        assertTrue(res.getNote().contains(
                SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED.getMessage()));
    }

    // ===== verifyVolRegistration ===========================================
    // ===== TC1 =====
    @Test
    void verifyVolRegistration_approve_success() {
        IdentityVerification identityVerification = validIdentityVerification();

        admin = new SystemAdmin();
        admin.setId(UUID.randomUUID());
        VolunteerRegistrationVerifyRequest request = approveRequest();
        request.setFullName(null);

        when(currentUserProvider.getId()).thenReturn(admin.getId());
        when(systemAdminRepository.getReferenceById(any())).thenReturn(admin);

        when(storageService.deleteFileAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        when(authClient.createAccount(any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        volunteerService.verifyVolRegistration(id, request);

        verify(volunteerRepository).save(any());
        verify(emailService).sendApproveRegisterVolAccountEmail(any(), any());
        verify(identityVerificationRepository).save(any());

        assertEquals(EVolunteerVerificationStatus.APPROVED,
                identityVerification.getStatus());
    }

    // ===== TC2 =====
    @Test
    void verifyVolRegistration_idNotExist_shouldThrow() {

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));
    }

    // ===== TC3 =====
    @Test
    void verifyVolRegistration_alreadyVerified_shouldThrow() {
        IdentityVerification identityVerification = validIdentityVerification();
        identityVerification.setStatus(EVolunteerVerificationStatus.APPROVED);

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        AppException ex = assertThrows(AppException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));

        assertEquals(VolunteerErrorCode.REGISTRATION_VERIFIED.getCode(), ex.getCode());
    }

    // ===== TC5 =====
    @Test
    void verifyVolRegistration_cidUsed_shouldThrow() {
        IdentityVerification identityVerification = validIdentityVerification();

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        when(volunteerRepository.existsByCid(any())).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));
        assertEquals(VolunteerErrorCode.CID_USED.getCode(), ex.getCode());
    }

    // ===== TC4 =====
    @Test
    void verifyVolRegistration_emailUsed_shouldThrow() {
        IdentityVerification identityVerification = validIdentityVerification();

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        when(userRepository.existsByEmail(any())).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));
        assertEquals(VolunteerErrorCode.EMAIL_USED.getCode(), ex.getCode());
    }

    // ===== TC6 =====
    @Test
    void verifyVolRegistration_phoneUsed_shouldThrow() {
        IdentityVerification identityVerification = validIdentityVerification();

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        when(volunteerRepository.existsByPhone(any())).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));
        assertEquals(VolunteerErrorCode.PHONE_USED.getCode(), ex.getCode());
    }

    // ===== TC7 =====
    @Test
    void verifyVolRegistration_reject_success() {

        IdentityVerification identityVerification = validIdentityVerification();

        admin = new SystemAdmin();
        admin.setId(UUID.randomUUID());

        when(currentUserProvider.getId()).thenReturn(admin.getId());
        when(systemAdminRepository.getReferenceById(any())).thenReturn(admin);

        when(storageService.deleteFileAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));


        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        volunteerService.verifyVolRegistration(id, rejectRequest());

        verify(emailService)
                .sendRejectRegisterVolAccountEmail(
                        eq("vol@mail.com"),
                        eq("invalid info"));

        assertEquals(EVolunteerVerificationStatus.REJECTED,
                identityVerification.getStatus());
    }

    @Test
    void verifyVolRegistration_deleteFileFail_shouldThrow() {
        IdentityVerification identityVerification = validIdentityVerification();

        when(identityVerificationRepository.findById(id))
                .thenReturn(Optional.of(identityVerification));

        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("storage down"));

        when(storageService.deleteFileAsync(any()))
                .thenReturn(failed);

        when(authClient.createAccount(any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        assertThrows(RuntimeException.class,
                () -> volunteerService.verifyVolRegistration(id, approveRequest()));
    }

    // ======= getVolunteersByAdmin ================================
    @Test
    void getVolunteersByAdmin_avatarNull_shouldNotCallStorage() {
        VolunteerSimpleResponseForAdmin v = mockVolunteer(null);

        Page<VolunteerSimpleResponseForAdmin> page =
                new PageImpl<>(List.of(v));

        when(volunteerRepository.findVolunteersByAdmin(any(), any()))
                .thenReturn(page);

        var result = volunteerService.getVolunteersByAdmin(0, 10, null);

        assertThat(result.getContent().get(0).getAvatarUrl()).isNull();
        verify(storageService, never()).getSignedUrlAsync(any());
    }

    @Test
    void getVolunteersByAdmin_avatarValid_shouldReplaceWithSignedUrl() {
        VolunteerSimpleResponseForAdmin v = mockVolunteer("path");

        Page<VolunteerSimpleResponseForAdmin> page =
                new PageImpl<>(List.of(v));

        when(volunteerRepository.findVolunteersByAdmin(any(), any()))
                .thenReturn(page);

        CompletableFuture<String> future =
                CompletableFuture.completedFuture("signed-url");

        when(storageService.getSignedUrlAsync("path"))
                .thenReturn(future);

        var result = volunteerService.getVolunteersByAdmin(0, 10, null);

        assertThat(result.getContent().get(0).getAvatarUrl())
                .isEqualTo("signed-url");
    }

    // ================= getVolunteerActivitiesByAdmin =================
    @Test
    void getVolunteerActivitiesByAdmin_shouldReturnRepositoryResult() {
        UUID volunteerId = UUID.randomUUID();

        VolunteerActivitiesResponseForAdmin response =
                new VolunteerActivitiesResponseForAdmin();

        Page<VolunteerActivitiesResponseForAdmin> page =
                new PageImpl<>(List.of(response));

        when(volunteerRepository.getVolunteerActivitiesByAdmin(
                any(Pageable.class),
                eq(volunteerId)
        )).thenReturn(page);

        var result = volunteerService.getVolunteerActivitiesByAdmin(
                volunteerId,
                0,
                10
        );

        verify(volunteerRepository)
                .getVolunteerActivitiesByAdmin(any(Pageable.class), eq(volunteerId));

        assertThat(result).isEqualTo(page);
    }
    //======= createVolunteerAccountByAdmin =====================
    @Test
    void createVolunteerAccountByAdmin_success() {
        CreateVolunteerAccountByAdminRequest request = new CreateVolunteerAccountByAdminRequest();
        request.setEmail("test@gmail.com");
        request.setCid("123456789");
        request.setPhone("0123456789");
        request.setFullName("nguyen van a");

        UUID adminId = UUID.randomUUID();
        UUID volunteerId = UUID.randomUUID();

        SystemAdmin admin = new SystemAdmin();
        admin.setId(adminId);

        when(currentUserProvider.getId()).thenReturn(adminId);
        when(systemAdminRepository.getReferenceById(adminId)).thenReturn(admin);

        when(authClient.createAccount(any(), any(), any(), any()))
                .thenReturn(volunteerId);

        // mock unique check (không throw)
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(volunteerRepository.existsByCid(any())).thenReturn(false);
        when(volunteerRepository.existsByPhone(any())).thenReturn(false);

        volunteerService.createVolunteerAccountByAdmin(request);

        verify(volunteerRepository).save(any(Volunteer.class));
        verify(emailService).sendApproveRegisterVolAccountEmail(eq(request.getEmail()), any());

    }

    // ==== getVolunteerPublicInformation ===================================
    // ===== TC1 =====
    @Test
    void getVolunteerPublicInformation_success_full_data() {

        UUID volunteerId = UUID.randomUUID();

        UUID vid = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setVid(vid);
        volunteer.setFullName("Nguyen Van A");
        volunteer.setNickname("A");
        volunteer.setBio("bio");
        volunteer.setDob(LocalDate.now());
        volunteer.setAvatarUrl("avatar-path");
        volunteer.setCreditScore(10);
        volunteer.setAvgRating((short) 5);
        volunteer.setActivityCount(3);

        Certificate cert1 = new Certificate();
        cert1.setCertificatePath("cert1");

        Certificate cert2 = new Certificate();
        cert2.setCertificatePath("cert2");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(certificateRepository.findByVolunteerId(volunteerId))
                .thenReturn(List.of(cert1, cert2));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        when(storageService.getSignedUrlAsync("cert1"))
                .thenReturn(CompletableFuture.completedFuture("url1"));

        when(storageService.getSignedUrlAsync("cert2"))
                .thenReturn(CompletableFuture.completedFuture("url2"));

        VolunteerPublicInformationResponse res =
                volunteerService.getVolunteerPublicInformation(volunteerId);

        assertEquals(vid, res.getVid());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals(2, res.getCertificatesUrls().size());
    }

    // ===== TC2 =====
    @Test
    void getVolunteerPublicInformation_not_found() {

        UUID volunteerId = UUID.randomUUID();

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.getVolunteerPublicInformation(volunteerId));
    }

    // ===== TC3 =====
    @Test
    void getVolunteerPublicInformation_avatar_null() {

        UUID volunteerId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl(null);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(certificateRepository.findByVolunteerId(volunteerId))
                .thenReturn(Collections.emptyList());

        VolunteerPublicInformationResponse res =
                volunteerService.getVolunteerPublicInformation(volunteerId);

        assertNull(res.getAvatarUrl());
    }

    // ===== TC4 =====
    @Test
    void getVolunteerPublicInformation_no_certificates() {

        UUID volunteerId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl(null);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(certificateRepository.findByVolunteerId(volunteerId))
                .thenReturn(Collections.emptyList());

        VolunteerPublicInformationResponse res =
                volunteerService.getVolunteerPublicInformation(volunteerId);

        assertTrue(res.getCertificatesUrls().isEmpty());
    }

    // ==== getVolunteerAccountInformation ===================================
    // ===== TC1 =====
    @Test
    void getVolunteerAccountInformation_success_full_data() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        UUID vid = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setVid(vid);
        volunteer.setCid("CID123");
        volunteer.setEmail("test@mail.com");
        volunteer.setPhone("0123");
        volunteer.setPhoneVerified(true);
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setBio("bio");
        volunteer.setGender(true);
        volunteer.setDob(LocalDate.now());
        volunteer.setLevel((short) 1);
        volunteer.setAvatarUrl("avatar-path");
        volunteer.setAddress("addr");
        volunteer.setDetailAddress("detail");
        volunteer.setEmployStatus(EEmployStatus.EMPLOYED);
        volunteer.setWorkAddress("work");
        volunteer.setEducationLevel(EEducationLevel.COLLEGE);
        volunteer.setSid("SID");
        volunteer.setCreditScore(10);
        volunteer.setHonorScore(5);
        volunteer.setAvgRating((short) 4);
        volunteer.setActivityCount(3);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        VolunteerAccountInformationResponse res =
                volunteerService.getVolunteerAccountInformation();

        assertEquals(volunteerId, res.getId());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals(vid, res.getVid());
    }

    // ===== TC2 =====
    @Test
    void getVolunteerAccountInformation_not_found() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.getVolunteerAccountInformation());
    }

    // ===== TC3 =====
    @Test
    void getVolunteerAccountInformation_avatar_null() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl(null);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        VolunteerAccountInformationResponse res =
                volunteerService.getVolunteerAccountInformation();

        assertNull(res.getAvatarUrl());
    }

    // ===== TC4 =====
    @Test
    void getVolunteerAccountInformation_avatar_empty() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl("");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        VolunteerAccountInformationResponse res =
                volunteerService.getVolunteerAccountInformation();

        assertNull(res.getAvatarUrl());
    }

    // ==== updateVolunteerProfile ===================================
    // ===== TC1 =====
    @Test
    void updateVolunteerProfile_success_with_avatar() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl("old-avatar");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(storageService.deleteFileAsync("old-avatar"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.volunteerAvatar(eq(volunteerId), any()))
                .thenReturn("new-avatar-path");

        when(storageService.getUploadUrlAsync("new-avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateVolunteerProfileRequest request = validUpdateRequest();
        request.setAvatarExtension("png");

        UpdateVolunteerProfileResponse res =
                volunteerService.updateVolunteerProfile(request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-avatar-path", volunteer.getAvatarUrl());

        verify(volunteerRepository).save(volunteer);
    }

    // ===== TC2 =====
    @Test
    void updateVolunteerProfile_not_found() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.updateVolunteerProfile(validUpdateRequest()));
    }

    // ===== TC3 =====
    @Test
    void updateVolunteerProfile_no_avatar() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        UpdateVolunteerProfileRequest request = validUpdateRequest();
        request.setAvatarExtension(null);

        UpdateVolunteerProfileResponse res =
                volunteerService.updateVolunteerProfile(request);

        assertNull(res.getAvatarUploadUrl());

        verify(storageService, never()).deleteFileAsync(any());
        verify(storageService, never()).getUploadUrlAsync(any());
    }

    // ===== TC4 =====
    @Test
    void updateVolunteerProfile_blank_enum() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        Volunteer volunteer = new Volunteer();

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        UpdateVolunteerProfileRequest request = validUpdateRequest();
        request.setEmployStatus("");
        request.setEducationLevel("");

        volunteerService.updateVolunteerProfile(request);

        assertNull(volunteer.getEmployStatus());
        assertNull(volunteer.getEducationLevel());
    }

    // ==== updateVolunteerProfileBySystemAdmin ===================================
    @Test
    void updateVolunteerProfileBySystemAdmin_success_with_avatar() {

        UUID volunteerId = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setAvatarUrl("old-avatar");

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(storageService.deleteFileAsync("old-avatar"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.volunteerAvatar(eq(volunteerId), any()))
                .thenReturn("new-avatar-path");

        when(storageService.getUploadUrlAsync("new-avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateVolunteerProfileBySystemAdminRequest request = validAdminRequest();
        request.setAvatarExtension("png");
        request.setEmail("new@mail.com");
        request.setPhone("0123");

        UpdateVolunteerProfileResponse res =
                volunteerService.updateVolunteerProfileBySystemAdmin(volunteerId, request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-avatar-path", volunteer.getAvatarUrl());
        assertEquals("new@mail.com", volunteer.getEmail());
        assertEquals("0123", volunteer.getPhone());
    }

    // ==== getVolunteerAccountInformationByAdmin ===================================
    @Test
    void getVolunteerAccountInformationByAdmin_success_full_data() {

        UUID volunteerId = UUID.randomUUID();

        UUID vid = UUID.randomUUID();

        Volunteer volunteer = new Volunteer();
        volunteer.setVid(vid);
        volunteer.setCid("CID123");
        volunteer.setEmail("test@mail.com");
        volunteer.setPhone("0123");
        volunteer.setPhoneVerified(true);
        volunteer.setNickname("nick");
        volunteer.setFullName("name");
        volunteer.setBio("bio");
        volunteer.setGender(true);
        volunteer.setDob(LocalDate.now());
        volunteer.setLevel((short) 1);
        volunteer.setAvatarUrl("avatar-path");
        volunteer.setAddress("addr");
        volunteer.setDetailAddress("detail");
        volunteer.setEmployStatus(EEmployStatus.EMPLOYED);
        volunteer.setWorkAddress("work");
        volunteer.setEducationLevel(EEducationLevel.COLLEGE);
        volunteer.setSid("SID");
        volunteer.setCreditScore(10);
        volunteer.setHonorScore(5);
        volunteer.setAvgRating((short) 4);
        volunteer.setActivityCount(3);

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        VolunteerAccountInformationResponse res =
                volunteerService.getVolunteerAccountInformationByAdmin(volunteerId);

        assertEquals(volunteerId, res.getId());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals(vid, res.getVid());
    }

    // ==== updateVolunteerProfile ===================================
    // ===== TC1 =====
    @Test
    void registerVolunteerFace_success() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        User user = new User();
        user.setFaceRegistered(false);

        Volunteer volunteer = new Volunteer();
        volunteer.setId(volunteerId);
        volunteer.setFullName("Nguyen Van A");

        when(userRepository.findById(volunteerId))
                .thenReturn(Optional.of(user));

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(faceAuthClient.registerFaceBiometric(any(), eq(volunteerId), any()))
                .thenReturn(new RegisterFaceResponse(
                        true,
                        "",
                        "",
                        20,
                        1,
                        1,
                        true,
                        5
                ));

        volunteerService.registerVolunteerFace("device-1", mock(MultipartFile.class));

        assertTrue(user.isFaceRegistered());
        assertEquals("device-1", volunteer.getDeviceId());

        verify(userRepository).save(user);
        verify(volunteerRepository).save(volunteer);
    }

    // ===== TC2 =====
    @Test
    void registerVolunteerFace_user_not_found() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        when(userRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.registerVolunteerFace("device-1", mock(MultipartFile.class)));
    }

    // ===== TC3 =====
    @Test
    void registerVolunteerFace_already_registered() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        User user = new User();
        user.setFaceRegistered(true);

        when(userRepository.findById(volunteerId))
                .thenReturn(Optional.of(user));

        assertThrows(AppException.class,
                () -> volunteerService.registerVolunteerFace("device-1", mock(MultipartFile.class)));
    }

    // ===== TC4 =====
    @Test
    void registerVolunteerFace_volunteer_not_found() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        User user = new User();
        user.setFaceRegistered(false);

        when(userRepository.findById(volunteerId))
                .thenReturn(Optional.of(user));

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> volunteerService.registerVolunteerFace("device-1", mock(MultipartFile.class)));
    }

    // ===== TC5 =====
    @Test
    void registerVolunteerFace_api_fail() {

        when(currentUserProvider.getId()).thenReturn(volunteerId);

        User user = new User();
        user.setFaceRegistered(false);

        Volunteer volunteer = new Volunteer();
        volunteer.setFullName("Nguyen Van A");

        when(userRepository.findById(volunteerId))
                .thenReturn(Optional.of(user));

        when(volunteerRepository.findById(volunteerId))
                .thenReturn(Optional.of(volunteer));

        when(faceAuthClient.registerFaceBiometric(any(), eq(volunteerId), any()))
                .thenReturn(new RegisterFaceResponse(
                        false,
                        "",
                        "",
                        20,
                        1,
                        1,
                        true,
                        5
                ));

        volunteerService.registerVolunteerFace("device-1", mock(MultipartFile.class));

        assertFalse(user.isFaceRegistered());
        assertNull(volunteer.getDeviceId());

        verify(userRepository, never()).save(any());
        verify(volunteerRepository, never()).save(any());
    }
}
