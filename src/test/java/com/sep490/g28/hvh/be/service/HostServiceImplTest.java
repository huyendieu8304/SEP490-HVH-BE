package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
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
import com.sep490.g28.hvh.be.service.impl.HostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HostServiceImplTest {
    @Mock
    HostRepository hostRepository;
    @Mock
    OrganizationManagerRepository organizationManagerRepository;
    @Mock
    UserRepository userRepository;

    @Mock
    AuthClient authClient;
    @Mock
    EmailService emailService;

    @Mock
    StoragePathGenerator storagePathGenerator;
    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    StorageService storageService;

    @InjectMocks
    HostServiceImpl hostService;

    UUID orgManagerId;
    UUID hostId;
    OrganizationManager orgManager;


    @BeforeEach
    void setUp() {

        orgManagerId = UUID.randomUUID();
        hostId = UUID.randomUUID();

        Organization org = new Organization();
        org.setName("Org A");

        orgManager = new OrganizationManager();
        orgManager.setId(orgManagerId);
        orgManager.setOrganization(org);

        lenient().when(currentUserProvider.getId()).thenReturn(orgManagerId);
        lenient().when(organizationManagerRepository.getReferenceById(orgManagerId))
                .thenReturn(orgManager);
    }

    private CreateHostAccountRequest validRequest() {
        CreateHostAccountRequest r = new CreateHostAccountRequest();
        r.setCid("123456789012");
        r.setEmail("host@mail.com");
        r.setPhone("0901234567");
        r.setFullName("Nguyen Van A");
        r.setAddress("Ha Noi");
        r.setDetailAddress("Ba Dinh");
        return r;
    }

    private UpdateHostProfileRequest validUpdateHostRequest() {
        UpdateHostProfileRequest r = new UpdateHostProfileRequest();
        r.setFullName("Nguyen Van B");
        r.setAddress("Ha Noi");
        r.setDetailAddress("Ba Dinh");
        return r;
    }

    private UpdateHostProfileBySystemAdminRequest validUpdateHostProfileBySystemAdminRequest() {
        UpdateHostProfileBySystemAdminRequest r = new UpdateHostProfileBySystemAdminRequest();
        r.setFullName("Nguyen Van B");
        r.setAddress("Ha Noi");
        r.setDetailAddress("Ba Dinh");
        return r;
    }

    // ===== createHostAccount ============================================
    // ===== TC1 =====
    @Test
    void createHostAccount_success() {

        CreateHostAccountRequest req = validRequest();

        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(false);

        UUID hostId = UUID.randomUUID();
        when(authClient.createAccount(any(), any(), any(), any()))
                .thenReturn(hostId);

        hostService.createHostAccount(req);

        verify(hostRepository).save(any(Host.class));
        verify(emailService).sendCreateHostAccountEmail(
                eq("Org A"),
                eq(req.getEmail()),
                any()
        );
    }

    // ===== TC2 =====
    @Test
    void createHostAccount_emailUsed_shouldThrow() {

        CreateHostAccountRequest req = validRequest();

        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> hostService.createHostAccount(req)
        );

        assertEquals(HostErrorCode.EMAIL_USED.getCode(), ex.getCode());

        verify(authClient, never()).createAccount(any(), any(), any(), any());
        verify(hostRepository, never()).save(any());
        verify(emailService, never()).sendCreateHostAccountEmail(any(), any(), any());
    }

    // ===== TC3 =====
    @Test
    void createHostAccount_authFail_shouldThrow() {

        CreateHostAccountRequest req = validRequest();

        when(userRepository.existsByEmail(req.getEmail()))
                .thenReturn(false);

        when(authClient.createAccount(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("auth down"));

        assertThrows(RuntimeException.class,
                () -> hostService.createHostAccount(req));

        verify(hostRepository, never()).save(any());
        verify(emailService, never()).sendCreateHostAccountEmail(any(), any(), any());
    }

    // ================= getHostsByManager =================

    @Test
    void getHostsByManager_avatarNull_shouldNotCallStorage() {
        HostSimpleResponseForManager host = new HostSimpleResponseForManager();
        host.setAvatarUrl(null);

        Page<HostSimpleResponseForManager> page =
                new PageImpl<>(List.of(host));

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(hostRepository.getHostsByManager(any(), any(), any()))
                .thenReturn(page);

        var result = hostService.getHostsByManager(0, 10, null);

        assertThat(result.getContent().get(0).getAvatarUrl()).isNull();
        verify(storageService, never()).getSignedUrlAsync(any());
    }

    @Test
    void getHostsByManager_avatarValid_shouldReplaceWithSignedUrl() {
        HostSimpleResponseForManager host = new HostSimpleResponseForManager();
        host.setAvatarUrl("path");

        Page<HostSimpleResponseForManager> page =
                new PageImpl<>(List.of(host));

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(hostRepository.getHostsByManager(any(), any(), any()))
                .thenReturn(page);

        CompletableFuture<String> future = CompletableFuture.completedFuture("signed-url");
        when(storageService.getSignedUrlAsync("path")).thenReturn(future);

        var result = hostService.getHostsByManager(0, 10, null);

        assertThat(result.getContent().get(0).getAvatarUrl())
                .isEqualTo("signed-url");
    }

    @Test
    void getHostsByManager_asyncException_shouldReturnNullAvatar() {
        HostSimpleResponseForManager host = new HostSimpleResponseForManager();
        host.setAvatarUrl("path");

        Page<HostSimpleResponseForManager> page =
                new PageImpl<>(List.of(host));

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(hostRepository.getHostsByManager(any(), any(), any()))
                .thenReturn(page);

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new CompletionException(new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)));

        when(storageService.getSignedUrlAsync("path")).thenReturn(future);

        var result = hostService.getHostsByManager(0, 10, null);

        assertThat(result.getContent().get(0).getAvatarUrl()).isNull();
    }

    // ================= getHostInfoByManager =================
    @Test
    void getHostInfoByManager_notFound_shouldThrow() {
        UUID hostId = UUID.randomUUID();

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,() -> hostService.getHostInfoByManager(hostId));
    }

    @Test
    void getHostInfoByManager_success_shouldReturnData() {
        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);
        host.setCid("123");
        host.setEmail("a@gmail.com");
        host.setPhone("090");
        host.setFullName("A");
        host.setGender(true);
        host.setDob(LocalDate.now());
        host.setAvatarUrl("path");
        host.setAddress("addr");
        host.setDetailAddress("detail");
        host.setCreatedAt(OffsetDateTime.now());

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        when(storageService.getSignedUrl("path"))
                .thenReturn("signed-url");

        var result = hostService.getHostInfoByManager(hostId);

        assertThat(result.getId()).isEqualTo(hostId);
        assertThat(result.getAvatarUrl()).isEqualTo("signed-url");

        verify(storageService).getSignedUrl("path");
    }

    @Test
    void getHostInfoByManager_asyncException_shouldReturnNullAvatar() {
        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);
        host.setAvatarUrl("path");

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new CompletionException(new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)));

        when(storageService.getSignedUrl("path"))
                .thenThrow(new CompletionException(new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)));

        var result = hostService.getHostInfoByManager(hostId);

        assertThat(result.getAvatarUrl()).isNull();
    }

    // ================= getHostActivitiesByManager =================
    @Test
    void getHostActivitiesByManager_shouldConvertDateRangeAndCallRepo() {
        UUID hostId = UUID.randomUUID();

        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 10);

        Page<HostActivitiesResponseForManager> page =
                new PageImpl<>(List.of());

        when(hostRepository.getHostActivitiesByManager(
                any(), any(), any(), any()
        )).thenReturn(page);

        var result = hostService.getHostActivitiesByManager(
                hostId, 0, 10, from, to
        );

        verify(hostRepository).getHostActivitiesByManager(
                eq(hostId),
                any(Pageable.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        );

        assertThat(result).isEqualTo(page);
    }

    // ================= updateHostProfile =================
    // ===== TC1 =====
    @Test
    void updateHostProfile_success_with_avatar() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        Host host = new Host();
        host.setAvatarUrl("old-avatar");

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        when(storageService.deleteFileAsync("old-avatar"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.hostAvatar(eq(hostId), any()))
                .thenReturn("new-avatar-path");

        when(storageService.getUploadUrlAsync("new-avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateHostProfileRequest request = validUpdateHostRequest();
        request.setAvatarExtension("png");

        UpdateHostProfileResponse res =
                hostService.updateHostProfile(request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-avatar-path", host.getAvatarUrl());

        verify(hostRepository).save(host);
    }

    // ===== TC2 =====
    @Test
    void updateHostProfile_not_found() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> hostService.updateHostProfile(validUpdateHostRequest()));
    }

    // ================= getHostAccountInformation =================
    // ===== TC1 =====
    @Test
    void getHostAccountInformation_success_full_data() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        Host host = new Host();
        host.setCid("CID123");
        host.setEmail("mail@test.com");
        host.setPhone("0123");
        host.setFullName("Host Name");
        host.setGender(true);
        host.setDob(LocalDate.now());
        host.setAvatarUrl("avatar-path");
        host.setAddress("addr");
        host.setDetailAddress("detail");

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        when(storageService.getSignedUrlAsync("avatar-path"))
                .thenReturn(CompletableFuture.completedFuture("avatar-url"));

        HostAccountInformationResponse res =
                hostService.getHostAccountInformation();

        assertEquals(hostId, res.getId());
        assertEquals("avatar-url", res.getAvatarUrl());
        assertEquals("CID123", res.getCid());
    }

    // ===== TC2 =====
    @Test
    void getHostAccountInformation_not_found() {

        when(currentUserProvider.getId()).thenReturn(hostId);

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> hostService.getHostAccountInformation());
    }

    // ================= getHostsOfOrganizationBySystemAdmin =================
    @Test
    void getHostsOfOrganizationBySystemAdmin_success() {

        UUID orgId = UUID.randomUUID();

        HostSimpleResponseForSystemAdmin h1 = new HostSimpleResponseForSystemAdmin();
        h1.setAvatarUrl(null);

        HostSimpleResponseForSystemAdmin h2 = new HostSimpleResponseForSystemAdmin();
        h2.setAvatarUrl("path-2");

        HostSimpleResponseForSystemAdmin h3 = new HostSimpleResponseForSystemAdmin();
        h3.setAvatarUrl("path-3");

        List<HostSimpleResponseForSystemAdmin> list = List.of(h1, h2, h3);

        Page<HostSimpleResponseForSystemAdmin> page =
                new PageImpl<>(list);

        when(hostRepository.getHostsOfOrganizationBySystemAdmin(eq(orgId), any(), any()))
                .thenReturn(page);

        when(storageService.getSignedUrlAsync("path-2"))
                .thenReturn(CompletableFuture.completedFuture("url-2"));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("fail"));

        when(storageService.getSignedUrlAsync("path-3"))
                .thenReturn(failedFuture);

        Page<HostSimpleResponseForSystemAdmin> result =
                hostService.getHostsOfOrganizationBySystemAdmin(0, 10, orgId, null);

        List<HostSimpleResponseForSystemAdmin> content = result.getContent();

        // h1: null stays null
        assertNull(content.get(0).getAvatarUrl());

        // h2: replaced
        assertEquals("url-2", content.get(1).getAvatarUrl());

        // h3: failed → null
        assertNull(content.get(2).getAvatarUrl());
    }

    // ================= getHostInfoBySystemAdmin =================
    // ===== TC1 =====
    @Test
    void getHostInfoBySystemAdmin_success() {

        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);
        host.setCid("CID123");
        host.setEmail("test@mail.com");
        host.setPhone("0123");
        host.setFullName("Host Name");
        host.setGender(true);
        host.setDob(LocalDate.now());
        host.setAvatarUrl("avatar-path");
        host.setAddress("addr");
        host.setDetailAddress("detail");
        host.setCreatedAt(OffsetDateTime.now());

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        when(storageService.getSignedUrl("avatar-path"))
                .thenReturn("signed-url");

        HostInfoResponseForSystemAdmin res =
                hostService.getHostInfoBySystemAdmin(hostId);

        assertEquals(hostId, res.getId());
        assertEquals("signed-url", res.getAvatarUrl());
        assertEquals("CID123", res.getCid());
    }

    // ===== TC2 =====
    @Test
    void getHostInfoBySystemAdmin_not_found() {

        UUID hostId = UUID.randomUUID();

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> hostService.getHostInfoBySystemAdmin(hostId));
    }

    // ================= getHostActivitiesBySystemAdmin =================
    @Test
    void getHostActivitiesBySystemAdmin_success() {

        UUID hostId = UUID.randomUUID();

        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2025, 1, 2);

        Page<HostActivitiesResponseForSystemAdmin> page =
                new PageImpl<>(Collections.emptyList());

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        when(hostRepository.getHostActivitiesBySystemAdmin(
                eq(hostId), any(), fromCaptor.capture(), toCaptor.capture()))
                .thenReturn(page);

        Page<HostActivitiesResponseForSystemAdmin> result =
                hostService.getHostActivitiesBySystemAdmin(hostId, 0, 10, fromDate, toDate);

        assertNotNull(result);

        OffsetDateTime from = fromCaptor.getValue();
        OffsetDateTime to = toCaptor.getValue();

        // from = start of day
        assertEquals(0, from.getHour());
        assertEquals(0, from.getMinute());

        // to = end of day
        assertEquals(23, to.getHour());
        assertEquals(59, to.getMinute());

        // timezone offset (VN = +7)
        assertEquals(7 * 60, from.getOffset().getTotalSeconds() / 60);
    }

    // ================= updateHostProfileBySystemAdmin =================
    // ===== TC1 =====
    @Test
    void updateHostProfileBySystemAdmin_success_with_avatar() {

        UUID hostId = UUID.randomUUID();

        Host host = new Host();
        host.setId(hostId);
        host.setAvatarUrl("old-path");

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.of(host));

        when(storageService.deleteFileAsync("old-path"))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(storagePathGenerator.hostAvatar(eq(hostId), any()))
                .thenReturn("new-path");

        when(storageService.getUploadUrlAsync("new-path"))
                .thenReturn(CompletableFuture.completedFuture("upload-url"));

        UpdateHostProfileBySystemAdminRequest request = validUpdateHostProfileBySystemAdminRequest();
        request.setAvatarExtension("jpg");

        UpdateHostProfileResponse res =
                hostService.updateHostProfileBySystemAdmin(hostId, request);

        assertEquals("upload-url", res.getAvatarUploadUrl());
        assertEquals("new-path", host.getAvatarUrl());

        verify(hostRepository).save(host);
    }

    // ===== TC2 =====
    @Test
    void updateHostProfileBySystemAdmin_not_found() {

        UUID hostId = UUID.randomUUID();

        when(hostRepository.findById(hostId))
                .thenReturn(Optional.empty());

        assertThrows(AppException.class,
                () -> hostService.updateHostProfileBySystemAdmin(hostId, validUpdateHostProfileBySystemAdminRequest()));
    }
}
