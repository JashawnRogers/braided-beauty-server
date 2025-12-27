package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.records.FrontendProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(FrontendProps.class)
@Configuration
public class PropsConfig {
}
