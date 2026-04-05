package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.OAuthIdentity;
import com.braided_beauty.braided_beauty.services.OAuthUserService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.Duration;

@Configuration
public class OAuthHandlersConfig {
    @Value("${app.cookies.secure}")
    private boolean secureCookie;

    @Value("${app.cookies.same-site}")
    private String sameSite;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    private static final Logger log = LoggerFactory.getLogger(OAuthHandlersConfig.class);

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(OAuthUserService oAuthUserService, RefreshTokenService refreshTokenService) {
        return (req, res, authentication) -> {
            // Convert provider Authentication to AppUserPrincipal based Authentication
            OAuthIdentity id = oAuthUserService.extractIdentity(authentication);

            User user = oAuthUserService.upsertFromOauth(id);

            var issued = refreshTokenService.issueForUser(user.getId(), "web");
            addRefreshToken(res, issued.getRefreshToken());


            log.info("{}/auth/callback", frontendBaseUrl);

            // Deliver accessToken back to frontend
            res.sendRedirect(frontendBaseUrl + "/auth/callback");
        };
    }

    private void addRefreshToken(HttpServletResponse res, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
