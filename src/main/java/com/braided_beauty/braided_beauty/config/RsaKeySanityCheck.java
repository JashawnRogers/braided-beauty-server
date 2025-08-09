package com.braided_beauty.braided_beauty.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    @PostConstruct
    public void checkKeys() throws IOException {
        try (var privIn = privateKeyResource.getInputStream();
             var pubIn = publicKeyResource.getInputStream()) {

            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(privIn);
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(pubIn);

            log.info("✅ RSA private key loaded: {} bits", privateKey.getModulus().bitLength());
            log.info("✅ RSA public key loaded: {} bits", publicKey.getModulus().bitLength());
        }
    }
}