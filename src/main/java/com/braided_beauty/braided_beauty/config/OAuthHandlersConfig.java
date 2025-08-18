package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.time.Duration;

@Configuration
public class OAuthHandlersConfig {

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(AuthService authService, JwtService jwtService,
                                                      RefreshTokenService refreshTokenService) {
        return (req, res, oauthAuth) -> {
            var appAuth = authService.toAppAuthentication(oauthAuth);
            String accessToken = jwtService.generateAccessToken(appAuth);

            var principal = (AppUserPrincipal) appAuth.getPrincipal();
            var issued = refreshTokenService.issueForUser(principal.getId(), "web");

            var cookie = ResponseCookie.from("refreshToken", issued.getRefreshToken())
                    .httpOnly(true).secure(true).sameSite("None").path("/")
                    .maxAge(Duration.ofDays(14)).build();
            res.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            res.setContentType("application/json");
            res.getWriter().write("{\"accessToken\":\"" + accessToken + "\"}");
        };
    }
}
