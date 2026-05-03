package com.sep490.g28.hvh.be.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.security.cert.Certificate;
import java.util.Base64;

@Component
@Getter
public class DigitalSignatureKeyProvider {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Certificate[] certificateChain;

    private static final Path PRIVATE_PATH = Path.of("keys/private_key.pem");
    private static final Path PUBLIC_PATH = Path.of("keys/public_key.pem");
    private static final Path CERT_PATH = Path.of("keys/certificate.crt");

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(PRIVATE_PATH) && Files.exists(PUBLIC_PATH)) {
                Security.addProvider(new BouncyCastleProvider());
                loadKeys();
                certificateChain = loadCertificateChain();

            }
        } catch (Exception e) {
            throw new RuntimeException("Init digital keys failed", e);
        }
    }

    private void loadKeys() throws Exception {
        this.privateKey = loadPrivateKey();
        this.publicKey = loadPublicKey();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(PRIVATE_PATH)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
//                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
//                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");


        byte[] decoded = Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    private PublicKey loadPublicKey() throws Exception {
        String pem = Files.readString(PUBLIC_PATH)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] decoded = Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
    }

    private Certificate[] loadCertificateChain() throws Exception {
        try (InputStream is = Files.newInputStream(CERT_PATH)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(is);
            return new Certificate[]{cert};
        }
    }
}
