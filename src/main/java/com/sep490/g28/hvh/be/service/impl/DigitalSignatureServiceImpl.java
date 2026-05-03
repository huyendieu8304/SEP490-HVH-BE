package com.sep490.g28.hvh.be.service.impl;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import com.sep490.g28.hvh.be.config.DigitalSignatureKeyProvider;
import com.sep490.g28.hvh.be.service.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class DigitalSignatureServiceImpl
        implements DigitalSignatureService
{

    private final DigitalSignatureKeyProvider keyProvider;


    @Override
    public byte[] signPdf(byte[] pdfBytes) {
        try (
                ByteArrayInputStream input = new ByteArrayInputStream(pdfBytes);
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {

            PdfReader reader = new PdfReader(input);

            PdfSigner signer = new PdfSigner(
                    reader,
                    output,
                    new StampingProperties()
            );

            Rectangle rect = new Rectangle(36, 648, 200, 100);

            signer.getSignatureAppearance()
                    .setReason("Volunteer Certificate")
                    .setLocation("System")
                    .setPageRect(rect)
                    .setPageNumber(1);

            signer.setFieldName("signature");

            IExternalSignature pks =
                    new PrivateKeySignature(
                            keyProvider.getPrivateKey(),
                            DigestAlgorithms.SHA256,
                            "BC"
                    );

            IExternalDigest digest = new BouncyCastleDigest();

            signer.signDetached(
                    digest,
                    pks,
                    keyProvider.getCertificateChain(),
                    null,
                    null,
                    null,
                    0,
                    PdfSigner.CryptoStandard.CADES
            );

            return output.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Sign PDF failed", e);
        }
    }

}


