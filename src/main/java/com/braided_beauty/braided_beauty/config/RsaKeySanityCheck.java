package com.braided_beauty.braided_beauty.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Component
@RequiredArgsConstructor
public class RsaKeySanityCheck {

    private static final Logger log = LoggerFactory.getLogger(RsaKeySanityCheck.class);

    @Value("${jwt.private-key}")
    private Resource privateKeyResource;

    @Value("${jwt.public-key}")
    private Resource publicKeyResource;

    @Value("${jwt.private-key-pem}")
    private String privateKeyPem;

    @Value("${jwt.public-key-pem}")
    private String publicKeyPem;

    @PostConstruct
    public void checkKeys() throws IOException {
        log.info("Checking JWT key resources...");
        log.info("Private key resource: {}", privateKeyResource);
        log.info("Public key resource: {}", publicKeyResource);


        try {
            RSAPrivateKey privateKey = loadPrivateKey();
            RSAPublicKey publicKey = loadPublicKey();


            log.info("RSA private key loaded: {} bits", privateKey.getModulus().bitLength());
            log.info("RSA public key loaded: {} bits", publicKey.getModulus().bitLength());
        } catch (Exception e) {
            log.error("Failed to load RSA keys. Private resource: {}, Public resource: {}", privateKeyResource, publicKeyResource);
            throw e;
        }
    }

    private RSAPrivateKey loadPrivateKey() throws IOException {
        if (StringUtils.hasText(privateKeyPem)) {
            try (var in = new ByteArrayInputStream(privateKeyPem.getBytes(StandardCharsets.UTF_8))) {
                return RsaKeyConverters.pkcs8().convert(in);
            }
        }

        try (var in = privateKeyResource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(in);
        }
    }

    private RSAPublicKey loadPublicKey() throws IOException {
        if (StringUtils.hasText(publicKeyPem)) {
            try (var in = new ByteArrayInputStream(publicKeyPem.getBytes(StandardCharsets.UTF_8))) {
                return RsaKeyConverters.x509().convert(in);
            }
        }

        try (var in = publicKeyResource.getInputStream()) {
            return RsaKeyConverters.x509().convert(in);
        }
    }

}