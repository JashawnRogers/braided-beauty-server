package com.braided_beauty.braided_beauty.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI braidedBeautyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Braided Beauty Booking API")
                        .description("REST API documentation for the Braided Beauty booking platform")
                        .version("v1.0")
                );
    }
}
