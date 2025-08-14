package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class OAuthHandlersConfig {

    @Bean
    AuthenticationSuccessHandler oauth2SuccessHandler(AuthService authService, JwtService jwtService) {
        return (req, res, oauthAuth) -> {
            var appAuth = authService.toAppAuthentication(oauthAuth);
            String accessToken = jwtService.generateAccessToken(appAuth);
            res.setContentType("application/json");
            res.getWriter().write("{\"accessToken\":\"" + accessToken + "\"}");
        };
    }
}
