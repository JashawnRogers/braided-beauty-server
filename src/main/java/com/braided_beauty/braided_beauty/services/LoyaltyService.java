package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltySettingsDTO;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.LoyaltySettings;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.LoyaltyRecordRepository;
import com.braided_beauty.braided_beauty.repositories.LoyaltySettingsRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LoyaltyService {
    private final LoyaltySettingsRepository settingsRepo;
    private final LoyaltyRecordRepository recordRepo;
    private final LoyaltyRecordDtoMapper mapper;

    @Transactional
    public void awardSignUpBonus(User user) {
        var settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled()) return;

        var record = recordRepo.findById(user.getId())
                .orElseGet(() -> recordRepo.save(new LoyaltyRecord(user)));

        if (!record.isSignupBonusAwarded()) return;

        int bonus = settings.getSignUpBonusPoints() != null ? settings.getSignUpBonusPoints() : 0;
        if (bonus <= 0) return;

        record.addPoints(bonus);
        record.setSignupBonusAwarded(true);
        recordRepo.save(record);
    }

    @Transactional
    public void awardForCompletedAppointment(User user) {
        var settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled()) return;

        var record = recordRepo.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Loyalty record missing"));

        if (!record.getEnabled()) return;

        int earn = settings.getEarnPerAppointment() != null ? settings.getEarnPerAppointment() : 0;
        if (earn <= 0) return;

        record.addPoints(earn);
        recordRepo.save(record);
    }

    @Transactional
    public void redeem(User user, int pointsToRedeem) {
        if (pointsToRedeem <= 0) throw new IllegalArgumentException("Insufficient redemption points");

        var settings = settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found"));
        if (!settings.isProgramEnabled()) throw new ConflictException("Loyalty program disabled");

        var record = recordRepo.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("Loyalty record missing"));

        if (record.getPoints() < pointsToRedeem) throw new ConflictException("Insufficient redemption points");

        record.setPoints(record.getPoints() - pointsToRedeem);
        record.setRedeemedPoints(record.getRedeemedPoints() + pointsToRedeem);
        recordRepo.save(record);
    }

    @Transactional
    public LoyaltySettingsDTO updateSettings(LoyaltySettingsDTO dto) {
        var settings = settingsRepo.findAny()
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

        settingsRepo.save(settings);
        return mapper.toDTO(settings);
    }

    @Transactional
    public LoyaltySettingsDTO getSettings() {
        return mapper.toDTO(settingsRepo.findAny()
                .orElseThrow(() -> new NotFoundException("Settings object not found")));
    }
}
