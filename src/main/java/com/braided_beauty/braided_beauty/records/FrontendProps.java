package com.braided_beauty.braided_beauty.records;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProps(String baseUrl) {
}
