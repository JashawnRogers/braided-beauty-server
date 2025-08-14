package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtService jwtService;
    private final AuthService authService;
    private final OAuthHandlersConfig oAuthHandlersConfig;

    // Secured matchers include: "/oauth2/**", "/login/oauth2/**", "/api/v1/login/**", "/api/v1/auth/**"
    // Start the login by hitting: http://localhost:8080/oauth2/authorization/google
    // Note: the OAuth2 callback that Spring handles is under "/login/oauth2/**".

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2Chain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/v1/login/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/login/**").permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuthHandlersConfig.oauth2SuccessHandler(authService, jwtService))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/login/**"));

        return httpSecurity.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/service/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/stripe/webhook").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                );

        return httpSecurity.build();
    }
}
