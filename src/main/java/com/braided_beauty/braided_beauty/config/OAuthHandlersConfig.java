package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.OAuthIdentity;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.OAuthUserService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.Duration;

@Configuration
public class OAuthHandlersConfig {
    @Value("${app.cookies.secure}")
    private boolean secureCookie;

    @Value("${app.cookies.same-site}")
    private String sameSite;

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(OAuthUserService oAuthUserService, RefreshTokenService refreshTokenService) {
        return (req, res, authentication) -> {
            // Convert provider Authentication to AppUserPrincipal based Authentication
            OAuthIdentity id = oAuthUserService.extractIdentity(authentication);

            User user = oAuthUserService.upsertFromOauth(id);

            var issued = refreshTokenService.issueForUser(user.getId(), "web");
            addRefreshToken(res, issued.getRefreshToken());

            // Deliver accessToken back to frontend
            res.sendRedirect("http://localhost:5173/auth/callback");
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
