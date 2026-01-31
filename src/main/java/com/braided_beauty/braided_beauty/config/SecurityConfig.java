package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.*;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@AllArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final OAuthUserService oauthService;
    private final RefreshTokenService refreshTokenService;
    private final OAuthHandlersConfig oAuthHandlersConfig;

    // Secured matchers include: "/oauth2/**", "/login/oauth2/**", "/api/v1/login/**", "/api/v1/auth/**"
    // Start the login by hitting: http://localhost:8080/oauth2/authorization/google
    // Note: the OAuth2 callback that Spring handles is under "/login/oauth2/**".

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        //Read from "roles" instead of "scope"
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");

        return jwt -> {
            var authorities = authoritiesConverter.convert(jwt);

            UUID userId = UUID.fromString(jwt.getSubject());
            String name = jwt.getClaimAsString("name");
            String email = jwt.getClaimAsString("email");

            Boolean enabledClaim = jwt.getClaimAsBoolean("enabled");
            boolean enabled = enabledClaim == null ? true : enabledClaim;
            AppUserPrincipal principal = new AppUserPrincipal(userId, email, name, authorities, null, enabled);

            // Wrap in Authentication Object
            // Credentials = jwt to keep access to raw token
            return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);

        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> {}) // Initialize with custom CorsConfig
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // OAuth entry
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Public API
                        .requestMatchers(
                                "/api/v1/service/**",
                                "/api/v1/category/**",
                                "/api/v1/availability/**",
                                "/api/v1/auth/**",
                                "/api/v1/appointments/book",
                                "/api/v1/appointments/guest/cancel",
                                "/api/v1/appointments/confirm",
                                "/api/v1/appointments/confirm/by-session",
                                "/api/v1/appointments/confirm/ics",
                                "/api/v1/appointments/confirm/ics/by-session",
                                "/api/v1/business/**",
                                "/api/v1/email/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhook/stripe").permitAll()
                        // Everything else secured
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuthHandlersConfig.oauth2SuccessHandler(oauthService, refreshTokenService))
                        .failureHandler((req, res, ex) -> {
                            ex.printStackTrace();
                            res.sendRedirect("http://localhost:5173/login?oauth=failed");
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

}