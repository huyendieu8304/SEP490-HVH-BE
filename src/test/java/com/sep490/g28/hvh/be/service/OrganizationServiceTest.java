package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.constant.EOrgRegistrationStatus;
import com.sep490.g28.hvh.be.constant.EOrgType;
import com.sep490.g28.hvh.be.dto.organization.request.OrganizationRegistrationVerifyRequest;
import com.sep490.g28.hvh.be.dto.organization.request.RegisterOrganizationRequest;
import com.sep490.g28.hvh.be.dto.organization.response.*;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.AppCommonErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.EventErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.OrganizationErrorCode;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.cache.OtpService;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.*;
import com.sep490.g28.hvh.be.service.impl.OrganizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    OrganizationRegistrationRepository organizationRegistrationRepository;

    @Mock
    OrganizationRepository organizationRepository;

    @Mock
    OrganizationManagerRepository organizationManagerRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    StorageService storageService;

    @Mock
    StoragePathGenerator storagePathGenerator;

    @Mock
    OtpService otpService;

    @Mock
    SystemAdminRepository systemAdminRepository;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    AuthClient authClient;

    @Mock
    EmailService emailService;

    @Mock
    HostRepository hostRepository;

    @Mock
    EventRepository eventRepository;

    @InjectMocks
    OrganizationServiceImpl organizationService;

    UUID id;
    SystemAdmin admin;
    UUID orgId;

    @BeforeEach
    void setup() {

        id = UUID.randomUUID();
        orgId = UUID.randomUUID();

    }

    private RegisterOrganizationRequest validRegisterOrganizationRequest() {
        RegisterOrganizationRequest req = new RegisterOrganizationRequest();
        req.setOtp("123456");
        req.setName("Tổ chức A");
        req.setDhaRegistered(true);
        req.setOrgType("SOCIAL_ORGANIZATION");
        req.setOrgIntroduction("Tổ chức tình nguyện");
        req.setManagerFullName("Nguyễn Quang A");
        req.setManagerCid("021304883103");
        req.setManagerPhone("0312786343");
        req.setManagerEmail("nguyenquanga@gmail.com");
        req.setManagerCidFrontExtension(".png");
        req.setManagerCidBackExtension(".png");
        req.setManagerCidHoldingExtension(".png");
        req.setLegalDocumentsExtensions(".png .pdf .jpg");
        req.setOtherEvidencesExtensions(".png .pdf .jpg");
        req.setApplicationReason("Yêu cầu đăng ký");
        return req;
    }

    private OrganizationRegistration validOrganizationRegistration() {
        OrganizationRegistration organizationRegistration = new OrganizationRegistration();
        organizationRegistration.setId(UUID.randomUUID());
        organizationRegistration.setName("Tổ chức B");
        organizationRegistration.setDhaRegistered(false);
        organizationRegistration.setOrgType(EOrgType.GOVERNMENT_AGENCY_BASED);
        organizationRegistration.setOrgIntroduction("Giới thiệu về tổ chức B");
        organizationRegistration.setManagerFullName("Nguyễn Văn C");
        organizationRegistration.setManagerCid("1234567");
        organizationRegistration.setManagerPhone("0936");
        organizationRegistration.setManagerEmail("org@mail.com");
        organizationRegistration.setApplicationReason("Yêu cầu đăng ký vào hệ thống");
        organizationRegistration.setManagerCidFront("f1");
        organizationRegistration.setManagerCidBack("f2");
        organizationRegistration.setManagerCidHolding("f3");
        organizationRegistration.setLegalDocument("f7 f8 f9");
        organizationRegistration.setOtherEvidences("f4 f5 f6");
        return organizationRegistration;
    }

    private OrganizationRegistrationVerifyRequest approveRequest() {
        OrganizationRegistrationVerifyRequest req = new OrganizationRegistrationVerifyRequest();
        req.setApprove(true);
        return req;
    }

    private OrganizationRegistrationVerifyRequest rejectRequest() {
        OrganizationRegistrationVerifyRequest req = new OrganizationRegistrationVerifyRequest();
        req.setApprove(false);
        req.setRejectionReason("Thông tin chưa đầy đủ");
        return req;
    }

    private Object[] mockOrgRow() {
        return new Object[]{
                UUID.randomUUID(),
                "Organization A",
                "SOCIAL_ORGANIZATION",
                15
        };
    }

    private Object[] mockOrgRow1() {
        return new Object[]{
                UUID.randomUUID(),
                "Organization A",
                "GOVERNMENT_AGENCY_BASED",
                15
        };
    }

    private Organization mockOrganization() {

        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Test Org");
        org.setCreatedAt(OffsetDateTime.now());
        org.setLegalDocument(null);
        org.setOtherEvidences(null);

        return org;
    }

    private OrganizationManager mockManager() {

        OrganizationManager manager = new OrganizationManager();
        manager.setId(UUID.randomUUID());
        manager.setFullName("Manager Name");
        manager.setEmail("manager@test.com");
        manager.setPhone("0123456789");
        manager.setCid("123456");

        return manager;
    }

    // ==== registerOrganization ===================================
    // ===== TC1 =====
    @Test
    void register_org_success() {
        RegisterOrganizationRequest request = validRegisterOrganizationRequest();
        when(otpService.verifyOrgRegistrationOtp(request.getManagerEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(false);

        when(storagePathGenerator.orgRegistrationCidFront(any(), any())).thenReturn("front-path");
        when(storagePathGenerator.orgRegistrationCidBack(any(), any())).thenReturn("back-path");
        when(storagePathGenerator.orgRegistrationCidHolding(any(), any())).thenReturn("holding-path");
        when(storagePathGenerator.orgRegistrationLegalDocuments(any(), anyInt(), any()))
                .thenReturn("legal-docs-path-1");
        when(storagePathGenerator.orgRegistrationOtherEvidences(any(), anyInt(), any()))
                .thenReturn("evidences-path-1");

        when(storageService.getUploadUrlAsync("front-path"))
                .thenReturn(CompletableFuture.completedFuture("front-url"));
        when(storageService.getUploadUrlAsync("back-path"))
                .thenReturn(CompletableFuture.completedFuture("back-url"));
        when(storageService.getUploadUrlAsync("holding-path"))
                .thenReturn(CompletableFuture.completedFuture("holding-url"));
        when(storageService.getUploadUrlAsync("legal-docs-path-1"))
                .thenReturn(CompletableFuture.completedFuture("legal-docs-url-1"));
        when(storageService.getUploadUrlAsync("evidences-path-1"))
                .thenReturn(CompletableFuture.completedFuture("evidences-url-1"));

        RegisterOrganizationResponse response =
                organizationService.registerOrganization(request);

        verify(organizationRegistrationRepository).save(any(OrganizationRegistration.class));

        assertEquals("front-url", response.getManagerCidFrontUploadUrl());
        assertEquals("back-url", response.getManagerCidBackUploadUrl());
        assertEquals("holding-url", response.getManagerCidHoldingUploadUrl());
        assertEquals("legal-docs-url-1", response.getLegalDocumentsUploadUrls().getFirst());
        assertEquals("evidences-url-1", response.getOtherEvidencesUploadUrls().getFirst());
    }

    // ===== TC2 =====
    @Test
    void register_org_fail_invalid_otp() {
        doThrow(new AppException(AppCommonErrorCode.OTP_INVALID))
                .when(otpService)
                .verifyOrgRegistrationOtp(any(), any());

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.registerOrganization(validRegisterOrganizationRequest())
        );
        assertEquals(AppCommonErrorCode.OTP_INVALID.getCode(), ex.getCode());

        verify(organizationRegistrationRepository, never()).save(any());
    }

    // ===== TC3 =====
    @Test
    void register_org_fail_email_used() {
        RegisterOrganizationRequest request = validRegisterOrganizationRequest();
        when(otpService.verifyOrgRegistrationOtp(request.getManagerEmail(), request.getOtp()))
                .thenReturn(true);
        when(userRepository.existsByEmail(any())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.registerOrganization(request)
        );
        assertEquals(OrganizationErrorCode.EMAIL_USED.getCode(), ex.getCode());
        verify(organizationRegistrationRepository, never()).save(any());
    }

    // ==== getOrgRegistrations ======================================
    // ===== TC1 =====
    @Test
    void getOrgRegistrations_should_return_page_with_status_and_manager_email() {
        int pageNumber = 0;
        int pageSize = 10;
        String statusInput = "PENDING";
        String managerEmail = "nguyenvanA@gmail.com";

        OrganizationRegistration or = new OrganizationRegistration();
        or.setStatus(EOrgRegistrationStatus.PENDING);

        Page<OrganizationRegistration> mockPage =
                new PageImpl<>(List.of(or));

        when(organizationRegistrationRepository.search(
                eq(EOrgRegistrationStatus.PENDING),
                eq(managerEmail),
                any(Pageable.class)
        )).thenReturn(mockPage);

        Page<OrganizationRegistrationSimpleResponse> result =
                organizationService.getOrgRegistrations(pageNumber, pageSize, statusInput, managerEmail);

        assertEquals(1, result.getTotalElements());

        verify(organizationRegistrationRepository)
                .search(eq(EOrgRegistrationStatus.PENDING),
                        eq(managerEmail),
                        any(Pageable.class));
    }

    // ===== TC2 =====
    @Test
    void getOrgRegistrations_should_pass_null_status_when_input_blank() {
        int pageNumber = 0;
        int pageSize = 10;

        Page<OrganizationRegistration> mockPage =
                new PageImpl<>(List.of(new OrganizationRegistration()));

        when(organizationRegistrationRepository.search(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(mockPage);

        Page<OrganizationRegistrationSimpleResponse> result =
                organizationService.getOrgRegistrations(pageNumber, pageSize, "    ", null);

        assertEquals(1, result.getTotalElements());

        verify(organizationRegistrationRepository)
                .search(isNull(),
                        isNull(),
                        any(Pageable.class));
    }

    // ===== getOrgRegistrationDetails ============================================
    // ===== TC1 =====
    @Test
    void getOrgRegistrationDetails_success() {
        UUID id = UUID.randomUUID();

        OrganizationRegistration or = new OrganizationRegistration();
        or.setId(id);
        or.setName("Tổ chức B");
        or.setDhaRegistered(false);
        or.setOrgType(EOrgType.GOVERNMENT_AGENCY_BASED);
        or.setOrgIntroduction("Giới thiệu về tổ chức B");
        or.setManagerFullName("Nguyễn Văn C");
        or.setManagerCid("021304883103");
        or.setManagerPhone("0312786343");
        or.setManagerEmail("nguyenquanga@gmail.com");
        or.setApplicationReason("Yêu cầu đăng ký vào hệ thống");
        or.setStatus(EOrgRegistrationStatus.PENDING);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(or));

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        when(userRepository.existsByEmail(any())).thenReturn(false);

        OrganizationRegistrationDetailsResponse res =
                organizationService.getOrgRegistrationDetails(id);

        assertEquals("url", res.getManagerCidFrontUrl());
        assertNull(res.getNote());
    }

    // ===== TC2 =====
    @Test
    void getOrgRegistrationDetails_idNotExist_shouldThrowException() {
        UUID invalidId = UUID.randomUUID();
        when(organizationRegistrationRepository.findById(invalidId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.getOrgRegistrationDetails(invalidId)
        );

        assertEquals(OrganizationErrorCode.REGISTRATION_NOT_EXISTED.getCode(), ex.getCode());
    }

    // ===== TC3 =====
    @Test
    void getOrgRegistrationDetails_emailUsed_shouldAppendNote() {

        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        when(userRepository.existsByEmail(any())).thenReturn(true);

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("url"));

        OrganizationRegistrationDetailsResponse res =
                organizationService.getOrgRegistrationDetails(id);

        assertTrue(res.getNote().contains(
                OrganizationErrorCode.EMAIL_USED.getMessage()));
    }

    // ===== TC4 =====
    @Test
    void getOrgRegistrationDetails_statusNotPending_shouldReturnAdminOrgOrgManagerId() {

        OrganizationRegistration organizationRegistration = new OrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.APPROVED);

        UUID adminId = UUID.randomUUID();
        SystemAdmin admin = new SystemAdmin();
        admin.setId(adminId);
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        UUID orgManagerId = UUID.randomUUID();
        OrganizationManager orgManager = new OrganizationManager();
        orgManager.setId(orgManagerId);

        organizationRegistration.setReviewedBy(admin);
        organizationRegistration.setOrganization(org);
        organizationRegistration.setOrgManager(orgManager);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        OrganizationRegistrationDetailsResponse res =
                organizationService.getOrgRegistrationDetails(id);

        assertEquals(adminId, res.getAdminId());
        assertEquals(orgId, res.getOrganizationId());
        assertEquals(orgManagerId, res.getOrgManagerId());
    }

    // ===== TC5 =====
    @Test
    void getOrgRegistrationDetails_signedUrlFail_shouldSetNote() {

        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        when(organizationRegistrationRepository.findById(any()))
                .thenReturn(Optional.of(organizationRegistration));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)
        );

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(failedFuture);

        OrganizationRegistrationDetailsResponse res =
                organizationService.getOrgRegistrationDetails(id);

        assertTrue(res.getNote().contains(
                SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED.getMessage()));
    }

    // ===== verifyOrgRegistration ===========================================
    // ===== TC1 =====
    @Test
    void verifyOrgRegistration_approve_success() {
        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        admin = new SystemAdmin();
        admin.setId(UUID.randomUUID());

        when(currentUserProvider.getId()).thenReturn(admin.getId());
        when(systemAdminRepository.getReferenceById(any())).thenReturn(admin);

        when(storageService.deleteFileAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        when(authClient.createAccount(any(), any(), any(), any()))
                .thenReturn(UUID.randomUUID());

        organizationService.verifyOrgRegistration(id, approveRequest());

        verify(organizationRepository).save(any());
        verify(organizationManagerRepository).save(any());
        verify(emailService).sendApproveRegisterOrganizationEmail(any(), any(), any());
        verify(organizationRegistrationRepository).save(any());

        assertEquals(EOrgRegistrationStatus.APPROVED,
                organizationRegistration.getStatus());
    }

    // ===== TC2 =====
    @Test
    void verifyOrgRegistration_idNotExist_shouldThrow() {

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> organizationService.verifyOrgRegistration(id, approveRequest()));
    }

    // ===== TC3 =====
    @Test
    void verifyOrgRegistration_alreadyVerified_shouldThrow() {
        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.APPROVED);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.verifyOrgRegistration(id, approveRequest()));

        assertEquals(OrganizationErrorCode.REGISTRATION_VERIFIED.getCode(), ex.getCode());
    }

    // ===== TC4 =====
    @Test
    void verifyOrgRegistration_emailUsed_shouldThrow() {
        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        when(userRepository.existsByEmail(any())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.verifyOrgRegistration(id, approveRequest()));

        assertEquals(OrganizationErrorCode.EMAIL_USED.getCode(), ex.getCode());
    }

    // ===== TC5 =====
    @Test
    void verifyOrgRegistration_reject_success() {

        OrganizationRegistration organizationRegistration = validOrganizationRegistration();
        organizationRegistration.setStatus(EOrgRegistrationStatus.PENDING);

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        admin = new SystemAdmin();
        admin.setId(UUID.randomUUID());

        when(currentUserProvider.getId()).thenReturn(admin.getId());
        when(systemAdminRepository.getReferenceById(any())).thenReturn(admin);

        when(storageService.deleteFileAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        organizationService.verifyOrgRegistration(id, rejectRequest());

        verify(emailService)
                .sendRejectRegisterOrganizationEmail(
                        eq("org@mail.com"),
                        eq("Thông tin chưa đầy đủ")
                );

        assertEquals(EOrgRegistrationStatus.REJECTED,
                organizationRegistration.getStatus());
    }

    @Test
    void verifyOrgRegistration_deleteFileFail_shouldThrow() {
        OrganizationRegistration organizationRegistration = validOrganizationRegistration();

        when(organizationRegistrationRepository.findById(id))
                .thenReturn(Optional.of(organizationRegistration));

        CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("storage down"));

        assertThrows(RuntimeException.class,
                () -> organizationService.verifyOrgRegistration(id, approveRequest()));
    }

    // ==== getOrganizations ======================================
    // ===== TC1 =====
    @Test
    void getOrganizations_success() {

        int pageNumber = 0;
        int pageSize = 10;
        String name = "Org";

        List<String> orgTypes = List.of("SOCIAL_ORGANIZATION", "GOVERNMENT_AGENCY_BASED");

        List<Object[]> rawData = List.of(
                mockOrgRow(),
                mockOrgRow1()
        );

        when(organizationRepository.search(
                eq(name),
                eq(orgTypes),
                any(Pageable.class)
        )).thenReturn(rawData);

        Page<OrganizationSimpleResponse> result =
                organizationService.getOrganizations(pageNumber, pageSize, name, orgTypes);

        assertEquals(2, result.getContent().size());

        verify(organizationRepository)
                .search(eq(name), eq(orgTypes), any(Pageable.class));
    }

    // ===== TC2 =====
    @Test
    void getOrganizations_with_null_filters() {

        int pageNumber = 0;
        int pageSize = 10;

        List<Object[]> rawData = List.of(
                mockOrgRow(),
                mockOrgRow());

        when(organizationRepository.search(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(rawData);

        Page<OrganizationSimpleResponse> result =
                organizationService.getOrganizations(pageNumber, pageSize, null, null);

        assertEquals(2, result.getContent().size());

        verify(organizationRepository)
                .search(isNull(), isNull(), any(Pageable.class));
    }

    // ===== getOrganizationDetailsBySystemAdmin ============================================
    // ===== TC1 =====
    @Test
    void getOrganizationDetailsBySystemAdmin_success() {

        Organization org = mockOrganization();
        org.setAvatarImage("avatar");
        org.setCoverImage("cover");
        org.setLegalDocument("doc1 doc2");
        org.setOtherEvidences("ev1 ev2");
        OrganizationManager manager = mockManager();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(org));

        when(organizationManagerRepository.findByOrganizationId(orgId))
                .thenReturn(manager);

        when(hostRepository.countHostByOrganizationId(orgId))
                .thenReturn(5L);

        when(eventRepository.findAllByOrganizationId(orgId))
                .thenReturn(Collections.emptyList());

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        OrganizationDetailsResponseForSystemAdmin response =
                organizationService.getOrganizationDetailsBySystemAdmin(orgId);

        assertEquals(manager.getFullName(), response.getManagerName());
        assertEquals(manager.getEmail(), response.getManagerEmail());
        assertEquals("signed-url", response.getAvatarImageUrl());
        assertEquals("signed-url", response.getCoverImageUrl());
        assertEquals(5L, response.getTotalHosts());
    }

    // ===== TC2 =====
    @Test
    void getOrganizationDetailsBySystemAdmin_org_not_exist() {

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> organizationService.getOrganizationDetailsBySystemAdmin(orgId)
        );

        assertEquals(
                OrganizationErrorCode.ORGANIZATION_NOT_EXISTED.getCode(),
                ex.getCode()
        );
    }

    // ===== TC3 =====
    @Test
    void getOrganizationDetailsBySystemAdmin_no_manager_found() {

        Organization org = mockOrganization();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(org));

        when(organizationManagerRepository.findByOrganizationId(orgId))
                .thenReturn(null);

        when(hostRepository.countHostByOrganizationId(orgId))
                .thenReturn(0L);

        when(eventRepository.findAllByOrganizationId(orgId))
                .thenReturn(Collections.emptyList());

        OrganizationDetailsResponseForSystemAdmin response =
                organizationService.getOrganizationDetailsBySystemAdmin(orgId);

        assertNull(response.getManagerId());
        assertTrue(response.getNote()
                .contains(OrganizationErrorCode.NO_ORGANIZATION_MANAGER_FOUND.getMessage()));
    }

    // ===== getOrganizationDetails ============================================
    // ===== TC1 =====
    @Test
    void getOrganizationDetails_success() {

        Organization org = mockOrganization();
        org.setAvatarImage("avatar");
        org.setCoverImage("cover");
        OrganizationManager manager = mockManager();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(org));

        when(organizationManagerRepository.findByOrganizationId(orgId))
                .thenReturn(manager);

        when(eventRepository.findAllByOrganizationId(orgId))
                .thenReturn(Collections.emptyList());

        when(storageService.getSignedUrlAsync(any()))
                .thenReturn(CompletableFuture.completedFuture("signed-url"));

        OrganizationDetailsResponse response =
                organizationService.getOrganizationDetails(orgId);

        assertEquals(manager.getEmail(), response.getManagerEmail());
        assertEquals("signed-url", response.getAvatarImageUrl());
        assertEquals("signed-url", response.getCoverImageUrl());
    }

    // ===== TC2 =====
    @Test
    void getOrganizationDetails_org_not_exist() {

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.empty());

        assertThrows(
                AppException.class,
                () -> organizationService.getOrganizationDetails(orgId)
        );
    }

    // ===== TC3 =====
    @Test
    void getOrganizationDetails_no_manager_found() {

        Organization org = mockOrganization();

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(org));

        when(organizationManagerRepository.findByOrganizationId(orgId))
                .thenReturn(null);

        when(eventRepository.findAllByOrganizationId(orgId))
                .thenReturn(Collections.emptyList());

        OrganizationDetailsResponse response =
                organizationService.getOrganizationDetails(orgId);

        assertNull(response.getManagerId());
        assertTrue(response.getNote()
                .contains(OrganizationErrorCode.NO_ORGANIZATION_MANAGER_FOUND.getMessage()));
    }

    // ===== TC4 =====
    @Test
    void getOrganizationDetails_calculate_honor_hours() {

        Organization org = mockOrganization();
        OrganizationManager manager = mockManager();

        EventSession session = new EventSession();
        session.setStartDateTime(OffsetDateTime.now());
        session.setEndDateTime(OffsetDateTime.now().plusHours(3));

        Event event = new Event();
        event.setSessions(List.of(session));

        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(org));

        when(organizationManagerRepository.findByOrganizationId(orgId))
                .thenReturn(manager);

        when(eventRepository.findAllByOrganizationId(orgId))
                .thenReturn(List.of(event));

        OrganizationDetailsResponse response =
                organizationService.getOrganizationDetails(orgId);

        assertEquals(3, response.getTotalHonorHours());
        assertNull(response.getAvatarImageUrl());
        assertNull(response.getCoverImageUrl());
    }
}
