package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.Duration;
import java.util.UUID;

@Configuration
public class OAuthHandlersConfig {

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(AuthService authService, JwtService jwtService,
                                                      RefreshTokenService refreshTokenService) {
        return (req, res, authentication) -> {
            // Convert provider Authentication to AppUserPrincipal based Authentication
            Authentication appAuth = authService.toAppAuthentication(authentication);
            AppUserPrincipal principal = (AppUserPrincipal) appAuth.getPrincipal();

            UUID userId = principal.id();
            String email = principal.email();
            String name = principal.name();

            String accessToken = jwtService.generateAccessToken(userId, email, name, appAuth.getAuthorities());

            var issued = refreshTokenService.issueForUser(userId, "web");
            addRefreshToken(res, issued.getRefreshToken());

            // Deliver accessToken back to frontend
            res.sendRedirect("http://localhost:5173/auth/callback?token=" + accessToken);
        };
    }

    private void addRefreshToken(HttpServletResponse res, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(14))
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
