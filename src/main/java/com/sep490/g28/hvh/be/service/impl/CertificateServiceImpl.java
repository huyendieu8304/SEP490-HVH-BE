package com.sep490.g28.hvh.be.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.LoadState;
import com.sep490.g28.hvh.be.constant.ECertificateStatus;
import com.sep490.g28.hvh.be.dto.certificate.payload.CertificateContentPayload;
import com.sep490.g28.hvh.be.entity.Certificate;
import com.sep490.g28.hvh.be.entity.Event;
import com.sep490.g28.hvh.be.entity.Volunteer;
import com.sep490.g28.hvh.be.integration.storage.StoragePathGenerator;
import com.sep490.g28.hvh.be.integration.storage.StorageService;
import com.sep490.g28.hvh.be.repository.CertificateRepository;
import com.sep490.g28.hvh.be.repository.EventRepository;
import com.sep490.g28.hvh.be.repository.VolunteerRepository;
import com.sep490.g28.hvh.be.service.CertificateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import javax.imageio.ImageIO;



import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CertificateServiceImpl implements CertificateService {
    CertificateRepository certificateRepository;

    StorageService storageService;

    TemplateEngine templateEngine;
     StoragePathGenerator storagePathGenerator;

//    @Value("${front-end.web.baseUrl}")
    private String frontendBaseUrl = "http://localhost:8080/";
    private final VolunteerRepository volunteerRepository;
    private final EventRepository eventRepository;


    // TODO: validate user participated in event
    @Override
    @Transactional
    public String generate(UUID volId, UUID eventId) {

        Volunteer volunteer = volunteerRepository.findById(volId).isPresent()?volunteerRepository.findById(volId).get():null;
        Event event = eventRepository.findById(eventId).isPresent()?eventRepository.findById(eventId).get():null;

        //create record of Certificate
        Certificate cert = new Certificate();

        String certCode = generateCode();
        cert.setCode(certCode);
        String certPath = storagePathGenerator.volunteerCertificate(volunteer.getId(), certCode, ".pdf");
        cert.setCertificatePath(certPath);

        cert.setEvent(event);
        cert.setVolunteer(volunteer);
        cert.setStatus(ECertificateStatus.ACTIVE);
        certificateRepository.save(cert);

        //create content payload for the certificate
        CertificateContentPayload payload = new CertificateContentPayload();
        payload.setOrganizationName(event.getOrganization().getName());
        payload.setVolunteerFullName(volunteer.getFullName());
        payload.setVid(volunteer.getVid().toString());
        payload.setEventName(event.getName());
        payload.setHostFullName(event.getHost().getFullName());
        payload.setIssuedDate(LocalDate.now().toString());

        String verifyUrl = frontendBaseUrl + "/verify/certificate/" + certCode;
        payload.setVerifyUrl(verifyUrl);
        String qrBase64 = generateQrBase64(verifyUrl);
        payload.setQrBase64(qrBase64);


        // 3. render HTML
        String html = renderHtml(payload);

        // 4. generate PDF
        byte[] pdfBytes = generatePdf(html);

        // 5. upload file
        storageService.upload(pdfBytes,certPath);

        return certPath;
    }

    private String generateCode() {
        return "CERT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String renderHtml(CertificateContentPayload payload) {

        Context context = new Context();
        context.setVariable("certPayload", payload);

        return templateEngine.process("template-cert", context);
    }

    private byte[] generatePdf(String html) {
//        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
//
//            PdfRendererBuilder builder = new PdfRendererBuilder();
//
//            // font (fix tiếng Việt)
//            builder.useFont(
//                    new ClassPathResource("fonts/Roboto-Regular.ttf").getFile(),
//                    "Roboto"
//            );
//
//            builder.withHtmlContent(html, "classpath:/templates/template-cert.html");
//            builder.toStream(os);
//            builder.run();
//
//            return os.toByteArray();
//
//        } catch (Exception e) {
//            throw new RuntimeException("PDF error", e);
//        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();

            page.setContent(html);
            // chờ load xong (font, ảnh...)
            page.waitForLoadState(LoadState.NETWORKIDLE);

            page.setContent(html);

            return page.pdf(new Page.PdfOptions()
                    .setWidth("297mm")
                    .setHeight("210mm")
                    .setFormat("A4")
                    .setLandscape(true)
                    .setPrintBackground(true)
            );

        } catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    private String generateQrBase64(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200);

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



//    @Override
//    public VerifyCertificateResponse verify(String code) {
//
//        Certificate cert = certificateRepository.findByCode(code)
//                .orElseThrow(() -> new RuntimeException("INVALID"));
//
//        return VerifyCertificateResponse.builder()
//                .status(cert.getStatus().name())
//                .userId(cert.getUserId())
//                .eventId(cert.getEventId())
//                .issuedAt(cert.getIssuedAt())
//                .build();
//    }
//
//    @Override
//    public List<CertificateResponse> getByUser(UUID userId) {
//        return certificateRepository.findByUserId(userId)
//                .stream()
//                .map(c -> CertificateResponse.builder()
//                        .code(c.getCode())
//                        .fileUrl(c.getFileUrl())
//                        .build())
//                .toList();
//    }
//
//    @Override
//    @Transactional
//    public void revoke(UUID id) {
//        Certificate cert = certificateRepository.findById(id)
//                .orElseThrow();
//
//        cert.setStatus(CertificateStatus.REVOKED);
//    }

}
