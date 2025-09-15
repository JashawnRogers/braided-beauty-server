package com.braided_beauty.braided_beauty.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig {
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        var corsConfig = new org.springframework.web.cors.CorsConfiguration();
        corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:5173"));
        corsConfig.setAllowedMethods(java.util.List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        corsConfig.setAllowedHeaders(java.util.List.of("*"));
        corsConfig.setAllowCredentials(true); // <-- required for cookies
        corsConfig.setMaxAge(3600L);

        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}
