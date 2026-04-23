package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EVolunteerVerificationStatus;
import com.sep490.g28.hvh.be.dto.volunteer.request.RegisterVolunteerAccountRequest;
import com.sep490.g28.hvh.be.dto.volunteer.request.VolunteerRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.volunteer.response.RegisterVolunteerAccountResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationDetailsResponse;
import com.sep490.g28.hvh.be.dto.volunteer.response.VolunteerRegistrationSimpleResponse;
import com.sep490.g28.hvh.be.entity.IdentityVerification;
import com.sep490.g28.hvh.be.entity.SystemAdmin;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.VolunteerErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.faceServer.FaceAuthClient;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
}
