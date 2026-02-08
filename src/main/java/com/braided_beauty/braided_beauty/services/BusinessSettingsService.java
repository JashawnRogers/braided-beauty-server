package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.models.BusinessSettings;
import com.braided_beauty.braided_beauty.records.BusinessSettingsDTO;
import com.braided_beauty.braided_beauty.repositories.BusinessSettingsRepository;
import com.braided_beauty.braided_beauty.utils.PhoneNormalizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class BusinessSettingsService {
    private final BusinessSettingsRepository repository;

    @Transactional
    public BusinessSettingsDTO updateSettings(BusinessSettingsDTO dto) {
        BusinessSettings settings = getOrCreate();
        int ambassadorPercentage = Optional.ofNullable(dto.ambassadorDiscountPercent()).orElse(0);

        if (dto.appointmentBufferTime() != null) {
            settings.setAppointmentBufferTime(dto.appointmentBufferTime());
        }

        if (dto.companyAddress() != null && !dto.companyAddress().isBlank()) {
            settings.setCompanyAddress(dto.companyAddress());
        }

        if (dto.companyEmail() != null && !dto.companyEmail().isBlank()) {
            settings.setCompanyEmail(dto.companyEmail());
        }

        if (dto.companyPhoneNumber() != null && !dto.companyPhoneNumber().isBlank()) {
            String normalizedPhoneNumber = PhoneNormalizer.toE164(dto.companyPhoneNumber())
                    .orElse("");
            settings.setCompanyPhoneNumber(normalizedPhoneNumber);
        }

        if (ambassadorPercentage > 0 && ambassadorPercentage < 100) {
            settings.setAmbassadorDiscountPercent(ambassadorPercentage);
        }


        BusinessSettings saved = repository.save(settings);

        log.info("Ambassador Discount Percentage d={}", saved.getAmbassadorDiscountPercent());
        return BusinessSettingsDTO.builder()
                .appointmentBufferTime(saved.getAppointmentBufferTime())
                .companyAddress(saved.getCompanyAddress())
                .companyEmail(saved.getCompanyEmail())
                .companyPhoneNumber(saved.getCompanyPhoneNumber())
                .ambassadorDiscountPercent(saved.getAmbassadorDiscountPercent())
                .build();
    }

    public BusinessSettingsDTO getSettings() {
        BusinessSettings settings = getOrCreate();

        return BusinessSettingsDTO.builder()
                .appointmentBufferTime(settings.getAppointmentBufferTime())
                .companyAddress(settings.getCompanyAddress())
                .companyEmail(settings.getCompanyEmail())
                .companyPhoneNumber(settings.getCompanyPhoneNumber())
                .ambassadorDiscountPercent(settings.getAmbassadorDiscountPercent())
                .build();
    }

    @Transactional
    public BusinessSettings getOrCreate() {
        return repository.findById(BusinessSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    try {
                        return repository.save(new BusinessSettings()); // id defaults to SINGLETON_ID
                    } catch (DataIntegrityViolationException e) {
                        // Another request created it between find and save
                        return repository.findById(BusinessSettings.SINGLETON_ID)
                                .orElseThrow(() -> e);
                    }
                });
    }

    @Transactional(readOnly = true)
    public int getAppointmentBufferMinutes() {
        Integer minutes = getSettings().appointmentBufferTime();
        return minutes == null ? 0 : minutes;
    }
}
