package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.LoyaltySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoyaltySettingsRepository extends JpaRepository<LoyaltySettings, UUID> {
    @Query("SELECT s FROM LoyaltySettings s")
    Optional<LoyaltySettings> findAny();

    default LoyaltySettings getSingleton() {
        return findById(LoyaltySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("LoyaltySettings row is missing"));
    }

    default LoyaltySettings getOrCreateSingleton() {
        return findById(LoyaltySettings.SINGLETON_ID).orElseGet(() -> {
            var settings = new LoyaltySettings();
            settings.setId(LoyaltySettings.SINGLETON_ID);
            return save(settings);
        });
    }
}
