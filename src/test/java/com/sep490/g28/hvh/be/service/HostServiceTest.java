package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.dto.host.request.CreateHostAccountRequest;
import com.sep490.g28.hvh.be.entity.Host;
import com.sep490.g28.hvh.be.entity.Organization;
import com.sep490.g28.hvh.be.entity.OrganizationManager;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.HostErrorCode;
import com.sep490.g28.hvh.be.integration.authServer.AuthClient;
import com.sep490.g28.hvh.be.integration.email.EmailService;
import com.sep490.g28.hvh.be.repository.HostRepository;
import com.sep490.g28.hvh.be.repository.OrganizationManagerRepository;
import com.sep490.g28.hvh.be.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HostServiceTest {
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
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    HostService hostService;

    UUID orgManagerId;
    OrganizationManager orgManager;


    @BeforeEach
    void setUp() {

        orgManagerId = UUID.randomUUID();

        Organization org = new Organization();
        org.setName("Org A");

        orgManager = new OrganizationManager();
        orgManager.setId(orgManagerId);
        orgManager.setOrganization(org);

        when(currentUserProvider.getId()).thenReturn(orgManagerId);
        when(organizationManagerRepository.getReferenceById(orgManagerId))
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

}
