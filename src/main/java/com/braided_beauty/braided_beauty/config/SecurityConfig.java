package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.services.AuthService;
import com.braided_beauty.braided_beauty.services.JwtService;
import com.braided_beauty.braided_beauty.services.RefreshTokenService;
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
    private final JwtService jwtService;
    private final AuthService authService;
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

            // Build domain principal
            AppUserPrincipal principal = new AppUserPrincipal(userId, name, email, authorities);

            // Wrap in Authentication Object
            // Credentials = jwt to keep access to raw token
            return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);

        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2Chain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuthHandlersConfig.oauth2SuccessHandler(authService, jwtService, refreshTokenService))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/oauth2/**", "/login/oauth2/**", "/api/v1/auth/login/**"));

        return httpSecurity.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/",
                                "/index.html",
                                "/static/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/api/v1/service",
                                "/api/v1/service/**",
                                "/api/v1/category",
                                "/api/v1/auth/**",
                                "/error"

                        ).permitAll()

                        // User protected routes
                        .requestMatchers("/api/v1/user/dashboard/me",
                                "/api/v1/appointments/**",
                                "/api/v1/appointments/book",
                                "/api/v1/appointments/cancel",
                                "/api/v1/user/user/**",
                                "/api/v1/user/appointments",
                                "/api/v1/user/loyalty-points"
                                ).authenticated()
                                .requestMatchers("/api/v1/admin/**").authenticated()

                                // All other routes are protected
                                .anyRequest().authenticated()
//                        .requestMatchers(HttpMethod.GET, "/api/v1/service/**").permitAll()
//                        .requestMatchers(HttpMethod.GET,"/api/v1/**").permitAll()
//                        .requestMatchers(HttpMethod.PUT,"/api/v1/**").permitAll()
//                        .requestMatchers(HttpMethod.POST,"/api/v1/**").permitAll()
//                        .requestMatchers(HttpMethod.DELETE,"/api/v1/**").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments/**").permitAll()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/stripe/webhook").permitAll()
//                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return httpSecurity.build();
    }
}
