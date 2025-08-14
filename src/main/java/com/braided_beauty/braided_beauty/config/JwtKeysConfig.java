package com.braided_beauty.braided_beauty.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class JwtKeysConfig {

    // Load PEM files from application.properties
    @Value("${jwt.private-key}")
    private Resource privateKeyResource;

    @Value("${jwt.public-key}")
    private Resource publicKeyResources;

    // Parse PEMs into Java RSA key objects
    @Bean
    public RSAPrivateKey rsaPrivateKey() throws IOException {
        try (var in = privateKeyResource.getInputStream()) {
            return RsaKeyConverters.pkcs8().convert(in);
        }
    }

    @Bean
    public RSAPublicKey rsaPublicKey() throws IOException {
        try (var in = publicKeyResources.getInputStream()) {
            return RsaKeyConverters.x509().convert(in);
        }
    }

    // Create the JwtEncoder (used to SIGN tokens with the PRIVATE key)
    @Bean
    public JwtEncoder jwtEncoder(RSAPrivateKey privateKey, RSAPublicKey publicKey){
        // Wrap the RSA keys into a JWK (JSON Web Key)
        var jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("bb-key-1")
                .build();

        var jwkSource = (com.nimbusds.jose.jwk.source.JWKSource<SecurityContext>)
                (selector, context) -> selector.select(new JWKSet(jwk));

        return new NimbusJwtEncoder(jwkSource);
    }

    // Create the JwtDecoder (used to VERIFY tokens with the PUBLIC key)
    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey){
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
