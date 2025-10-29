package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.repositories.LoyaltySettingsRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LoyaltySettingsInitializer implements ApplicationRunner {
    private final LoyaltySettingsRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        repo.getOrCreateSingleton();
    }
}
