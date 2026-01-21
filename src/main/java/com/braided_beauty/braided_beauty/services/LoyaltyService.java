package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltySettingsDTO;
import com.braided_beauty.braided_beauty.enums.LoyaltyTier;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.LoyaltySettings;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.LoyaltyRecordRepository;
import com.braided_beauty.braided_beauty.repositories.LoyaltySettingsRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class LoyaltyService {
    private final LoyaltySettingsRepository settingsRepo;
    private final LoyaltyRecordRepository recordRepo;
    private final LoyaltyRecordDtoMapper mapper;
    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(LoyaltyService.class);

    @Transactional
    public void attachLoyaltyRecord(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        if (user.getLoyaltyRecord() != null) return;

        user.applyLoyaltyRecord(new LoyaltyRecord());
    }

    @Transactional
    public void awardSignUpBonus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));

        LoyaltySettings settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled())
            throw new ConflictException("Loyalty program disabled. Early exiting without awarding sign up bonus.");

        LoyaltyRecord record = recordRepo.findById(user.getId())
                .orElseGet(() -> {
                    attachLoyaltyRecord(userId);
                    return user.getLoyaltyRecord();
                });

        if (record.isSignupBonusAwarded()) return;

        int bonus = settings.getSignUpBonusPoints() != null
                && settings.getSignUpBonusPoints() > 0
                ? settings.getSignUpBonusPoints() : 0;

        record.addPoints(bonus);
        record.setSignupBonusAwarded(true);
        recordRepo.save(record);
    }

    @Transactional
    public void awardForCompletedAppointment(UUID userId) {
        var settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled())
            throw new IllegalStateException("Loyalty program disabled. Points not awarded");


        LoyaltyRecord record = recordRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Loyalty record missing"));

        if (!record.getEnabled())
            throw new IllegalStateException("Loyalty record not enabled. Loyalty points not awarded.");

        int earn = settings.getSignUpBonusPoints() != null
                && settings.getSignUpBonusPoints() > 0
                ? settings.getSignUpBonusPoints() : 0;

        record.addPoints(earn);
        record.setUpdatedAt(LocalDateTime.now());
        recordRepo.save(record);
    }

    @Transactional
    public void redeem(UUID userId, int pointsToRedeem) {
        LoyaltySettings settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled()) throw new ConflictException("Loyalty program is disabled");

        LoyaltyRecord record = recordRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("Loyalty record missing"));

        record.redeemPoints(pointsToRedeem);
        recordRepo.save(record);
    }

    @Transactional
    public LoyaltySettingsDTO updateSettings(LoyaltySettingsDTO dto) {
        LoyaltySettings settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));

        if (dto.getEarnPerAppointment() != null) {
            settings.setEarnPerAppointment(dto.getEarnPerAppointment());
        }
        if (dto.getSignupBonusPoints() != null) {
            settings.setSignUpBonusPoints(dto.getSignupBonusPoints());
        }
        if (dto.getProgramEnabled() != null) {
            settings.setProgramEnabled(dto.getProgramEnabled());
        }
        if (dto.getGoldTierThreshold() != null) {
            settings.setGoldTierThreshold(dto.getGoldTierThreshold());
        }
        if (dto.getSilverTierThreshold() != null){
            settings.setSilverTierThreshold(dto.getSilverTierThreshold());
        }
        if (dto.getBronzeTierThreshold() != null) {
            settings.setBronzeTierThreshold(dto.getBronzeTierThreshold());
        }

        settingsRepo.save(settings);
        return mapper.toDTO(settings);
    }

    public LoyaltySettingsDTO getSettings() {
        return mapper.toDTO(settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found")));
    }

    public LoyaltyTier calculateLoyaltyTier(int points) {
        LoyaltySettings settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));

        Integer goldTier = settings.getGoldTierThreshold();
        Integer silverTier = settings.getSilverTierThreshold();

        if (goldTier != null && points >= goldTier) return LoyaltyTier.GOLD;
        if (silverTier != null && points >= silverTier) return LoyaltyTier.SILVER;
        return LoyaltyTier.BRONZE;
    }
}
