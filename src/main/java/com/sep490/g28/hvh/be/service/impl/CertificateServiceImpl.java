package com.sep490.g28.hvh.be.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.microsoft.playwright.options.LoadState;
import com.sep490.g28.hvh.be.auth.CurrentUserProvider;
import com.sep490.g28.hvh.be.config.PlaywrightManager;
import com.sep490.g28.hvh.be.constant.ECertificateStatus;
import com.sep490.g28.hvh.be.dto.certificate.payload.CertificateContentPayload;
import com.sep490.g28.hvh.be.dto.certificate.response.VerifyCertificateResponse;
import com.sep490.g28.hvh.be.dto.certificate.response.VolunteerCertificateResponse;
import com.sep490.g28.hvh.be.entity.Certificate;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.exception.AppException;
import com.sep490.g28.hvh.be.exception.errorCodeImpl.CertificateErrorCode;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.CertificateRepository;
import com.sep490.g28.hvh.be.service.CertificateService;
import com.sep490.g28.hvh.be.service.DigitalSignatureService;
import com.sep490.g28.hvh.be.util.AsyncExceptionUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateServiceImpl implements CertificateService {
    CertificateRepository certificateRepository;

    StorageService storageService;

    TemplateEngine templateEngine;
    StoragePathGenerator storagePathGenerator;
    PlaywrightManager playwrightManager;

    CurrentUserProvider currentUserProvider;

    DigitalSignatureService digitalSignatureService;

    @Value("${front-end.web.cert-url}")
    @NonFinal
    String frontendCertUrl;

    @Override
    public Page<VolunteerCertificateResponse> getCertificatesByVolunteer(int pageNumber, int pageSize, String eventName) {
        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize
        );

        Page<VolunteerCertificateResponse> page = certificateRepository.findByVolunteerIdAndEventName(
                pageable,
                currentUserProvider.getId(),
                eventName
        );

        List<VolunteerCertificateResponse> content =
                page.getContent().stream()
                        .map(cert -> {
                            if (cert.getCertSignedUrl() == null){
                                return cert;
                            }
                            //get signed Url for certificate
                            String path = cert.getCertSignedUrl();
                            try {
                                String url = storageService.getSignedUrlAsync(path).join();
                                cert.setCertSignedUrl(url);
                            } catch (CompletionException e) {
                                cert.setCertSignedUrl(
                                        AsyncExceptionUtils.resolveExceptionReturnFallbackIfFileNotExisted(e, null)
                                );
                            }
                            return cert;

                        }).toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public VerifyCertificateResponse verifyCertificate(String certCode) {
        Certificate cert = certificateRepository.findByCode(certCode).orElseThrow(
                () -> new AppException(CertificateErrorCode.CERTIFICATE_NOT_EXISTED)
        );

        VerifyCertificateResponse response = new VerifyCertificateResponse();
        response.setCertSignedUrl(storageService.getSignedUrl(cert.getCertificatePath()));

        return response;
    }

    @Override
    @Transactional
    public void generateCertificate(Volunteer volunteer, Event event) {
        //create record of Certificate
        Certificate cert = buidCertificate(volunteer,event);

        //create content payload for the certificate
        CertificateContentPayload payload = buildCertificatePayload(volunteer, event,cert);

        //render HTML to prepare for generate pdf
        String html = renderHtml(payload);

        //generate PDF
        byte[] unsignedPdf = generatePdf(html);
        byte[] signedPdf = digitalSignatureService.signPdf(unsignedPdf);

        certificateRepository.save(cert);
        // 5. upload file
        storageService.upload(signedPdf, cert.getCertificatePath());
        log.info("Generate certificate successfully, certPath={}", cert.getCertificatePath());
    }

    private Certificate buidCertificate(Volunteer volunteer, Event event) {
        Certificate cert = new Certificate();

        String certCode = generateCode();
        cert.setCode(certCode);

        String certPath = storagePathGenerator.volunteerCertificate(volunteer.getId(), certCode, ".pdf");
        cert.setCertificatePath(certPath);

        cert.setEvent(event);
        cert.setVolunteer(volunteer);
        cert.setStatus(ECertificateStatus.ACTIVE);
        return cert;
    }

    private CertificateContentPayload buildCertificatePayload(Volunteer volunteer, Event event, Certificate cert) {
        CertificateContentPayload payload = new CertificateContentPayload();
        payload.setOrganizationName(event.getOrganization().getName());
        payload.setVolunteerFullName(volunteer.getFullName());
        payload.setVid(volunteer.getVid().toString());
        payload.setEventName(event.getName());
        payload.setHostFullName(event.getHost().getFullName());
        payload.setIssuedDate(LocalDate.now());

        String verifyUrl = frontendCertUrl + "/verify/certificate/" + cert.getCode();

        payload.setVerifyUrl(verifyUrl);

        //generate qr code for verifying
        String qrBase64 = generateQrBase64(verifyUrl);
        payload.setQrBase64(qrBase64);
        return payload;
    }

    private String generateCode() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return date + "-" + random;
    }

    private String renderHtml(CertificateContentPayload payload) {

        Context context = new Context();
        context.setVariable("certPayload", payload);

        return templateEngine.process("template-cert", context);
    }

    private byte[] generatePdf(String html) {

        try (com.microsoft.playwright.Page page = playwrightManager.newPage()) {
            //set html content for page
            page.setContent(html);
            // wait for loading image and fonts
            page.waitForLoadState(LoadState.NETWORKIDLE);
            return page.pdf(new com.microsoft.playwright.Page.PdfOptions()
                    .setFormat("A4")
                    .setWidth("297mm")
                    .setHeight("210mm")
                    .setLandscape(true)
                    .setPrintBackground(true)
            );
        }
    }

    private String generateQrBase64(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 110, 110);

            int width = matrix.getWidth();
            int height = matrix.getHeight();

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(image, "png", os);

            return Base64.getEncoder().encodeToString(os.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("QR error", e);
        }
    }

    @Override
    @Transactional
    public void generateCertificates(List<Volunteer> volunteers, Event event){
        List<Certificate> certificates = new ArrayList<>();

        Map<String, byte[]> pdfMap = new HashMap<>();

        for (Volunteer volunteer: volunteers) {
            //create record of Certificate
            Certificate cert = buidCertificate(volunteer,event);

            //create content payload for the certificate
            CertificateContentPayload payload = buildCertificatePayload(volunteer, event,cert);

            //render HTML to prepare for generate pdf
            String html = renderHtml(payload);

            //generate PDF
            byte[] pdfBytes = generatePdf(html);

            certificates.add(cert);
            pdfMap.put(cert.getCertificatePath(), pdfBytes);
        }

        certificateRepository.saveAll(certificates);

        pdfMap.forEach((certPath, pdfBytes) -> CompletableFuture.runAsync(() ->
            storageService.upload(pdfBytes, certPath)
        ));
        log.info("Generate certificate successfully for {} volunteers participated in event {} , eventId={}",
                volunteers.size(),
                event.getName(),
                event.getId()
                );

    }

//    @Override
//    @Transactional
//    public void revoke(UUID id) {
//        Certificate cert = certificateRepository.findById(id)
//                .orElseThrow();
//
//        cert.setStatus(CertificateStatus.REVOKED);
//    }

}
