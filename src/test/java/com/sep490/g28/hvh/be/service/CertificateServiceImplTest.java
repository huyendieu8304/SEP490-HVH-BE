package com.sep490.g28.hvh.be.service;

import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.config.PlaywrightManager;
import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import com.sep490.g28.hvh.be.entity.*;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.SupabaseErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.CertificateRepository;
import com.sep490.g28.hvh.be.service.impl.CertificateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CertificateServiceImplTest {

    @Mock
    CertificateRepository certificateRepository;

    @Mock
    StorageService storageService;

    @Mock
    TemplateEngine templateEngine;

    @Mock
    StoragePathGenerator storagePathGenerator;

    @Mock
    PlaywrightManager playwrightManager;
    @Mock
    CurrentUserProvider currentUserProvider;

    @InjectMocks
    @Spy
    CertificateServiceImpl service;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "frontendCertUrl", "http://fe");
    }


    private Volunteer mockVolunteer() {
        var v = mock(Volunteer.class);
        when(v.getId()).thenReturn(UUID.randomUUID());
        when(v.getFullName()).thenReturn("Name");
        when(v.getVid()).thenReturn(UUID.randomUUID());
        return v;
    }

    private Event mockEvent() {
        var e = mock(Event.class);
        var org = mock(Organization.class);
        var host = mock(Host.class);

        when(org.getName()).thenReturn("Org");
        when(host.getFullName()).thenReturn("Host");

        when(e.getOrganization()).thenReturn(org);
        when(e.getHost()).thenReturn(host);
        when(e.getName()).thenReturn("Event");
        lenient().when(e.getId()).thenReturn(UUID.randomUUID());
        return e;
    }

    // ================= getCertificatesByVolunteer =================

    @Test
    void  getCertificatesByVolunteer_validSignedUrl_shouldReturnSignedUrl() {
        var cert = new VolunteerCertificateResponse();
        cert.setCertSignedUrl("path");

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(certificateRepository.findByVolunteerIdAndEventName(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cert)));

        when(storageService.getSignedUrlAsync("path"))
                .thenReturn(CompletableFuture.completedFuture("signed"));

        var result = service.getCertificatesByVolunteer(0, 10, null);

        assertThat(result.getContent().getFirst().getCertSignedUrl()).isEqualTo("signed");
    }

    @Test
    void getCertificatesByVolunteer_asyncException_shouldReturnNullSignedUrl()  {
        var cert = new VolunteerCertificateResponse();
        cert.setCertSignedUrl("path");

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(certificateRepository.findByVolunteerIdAndEventName(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cert)));

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(
                new CompletionException(
                        new AppException(SupabaseErrorCode.STORAGE_FILE_NOT_EXISTED)
                )
        );
        when(storageService.getSignedUrlAsync("path")).thenReturn(future);

        var result = service.getCertificatesByVolunteer(0, 10, null);

        assertThat(result.getContent().get(0).getCertSignedUrl()).isNull();
    }

    @Test
    void getCertificatesByVolunteer_runtimeException_shouldThrow() {
        var cert = new VolunteerCertificateResponse();
        cert.setCertSignedUrl("path");

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(certificateRepository.findByVolunteerIdAndEventName(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cert)));

        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(
                new CompletionException(new RuntimeException("boom"))
        );

        when(storageService.getSignedUrlAsync("path")).thenReturn(future);

        assertThatThrownBy(() ->
                service.getCertificatesByVolunteer(0, 10, null)
        ).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getCertificatesByVolunteer_nullCertPath_shouldSkipProcessing(){
        var cert = new VolunteerCertificateResponse();
        cert.setCertSignedUrl(null);

        when(currentUserProvider.getId()).thenReturn(UUID.randomUUID());
        when(certificateRepository.findByVolunteerIdAndEventName(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(cert)));

        var result = service.getCertificatesByVolunteer(0, 10, null);

        assertThat(result.getContent().get(0).getCertSignedUrl()).isNull();
    }

    // ================= verifyCertificate =================

    @Test
    void  verifyCertificate_validCode_shouldReturnSignedUrl() {
        var cert = new Certificate();
        cert.setCertificatePath("path");

        when(certificateRepository.findByCode("code"))
                .thenReturn(Optional.of(cert));
        when(storageService.getSignedUrl("path"))
                .thenReturn("signed");

        var res = service.verifyCertificate("code");

        assertThat(res.getCertSignedUrl()).isEqualTo("signed");
    }

    @Test
    void verifyCertificate_notFound_shouldThrowAppException() {
        when(certificateRepository.findByCode("code"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCertificate("code"))
                .isInstanceOf(AppException.class);
    }

    // ================= generateCertificate =================
    @Test
    void generateCertificate_validInput_shouldSaveAndUploadFile() {
        var volunteer = mockVolunteer();
        var event = mockEvent();

        // mock path
        when(storagePathGenerator.volunteerCertificate(any(), any(), any()))
                .thenReturn("path");

        // mock template engine
        when(templateEngine.process(eq("template-cert"), any()))
                .thenReturn("<html></html>");

        // mock playwright
        com.microsoft.playwright.Page page = mock(com.microsoft.playwright.Page.class);

        when(playwrightManager.newPage()).thenReturn(page);
        doNothing().when(page).setContent(any());
        doNothing().when(page).waitForLoadState(any());
        when(page.pdf(any())).thenReturn(new byte[]{1,2,3});

        // IMPORTANT: mock close() vì try-with-resources
        doNothing().when(page).close();

        service.generateCertificate(volunteer, event);

        verify(certificateRepository).save(any());
        verify(storageService).upload((byte[]) any(), eq("path"));
    }

    // ================= generateCertificates =================

    @Test
    void generateCertificates_validList_shouldSaveAllAndUploadAsync() {
        var volunteer = mockVolunteer();
        var event = mockEvent();

        when(storagePathGenerator.volunteerCertificate(any(), any(), any()))
                .thenReturn("path");

        when(templateEngine.process(eq("template-cert"), any()))
                .thenReturn("<html></html>");

        com.microsoft.playwright.Page page = mock(com.microsoft.playwright.Page.class);

        when(playwrightManager.newPage()).thenReturn(page);
        doNothing().when(page).setContent(any());
        doNothing().when(page).waitForLoadState(any());
        when(page.pdf(any())).thenReturn(new byte[]{1});
        doNothing().when(page).close();

        service.generateCertificates(List.of(volunteer), event);

        verify(certificateRepository).saveAll(any());
        verify(storageService, timeout(1000).atLeastOnce())
                .upload((byte[]) any(), any());
    }

}
