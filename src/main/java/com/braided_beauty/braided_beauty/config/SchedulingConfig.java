package com.braided_beauty.braided_beauty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "braided-beauty")
@Getter
@Setter
public class SchedulingConfig {
    private int bufferMinutes;
}
