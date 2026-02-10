package com.braided_beauty.braided_beauty.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public Clock phoenixClock() {
        return Clock.system(ZoneId.of("America/Phoenix"));
    }
}
