package com.braided_beauty.braided_beauty.records;

import com.braided_beauty.braided_beauty.utils.PhoneConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.UUID;

@Builder
public record BusinessSettingsDTO(
        UUID id,
        @PhoneConstraint String companyPhoneNumber,
        String companyAddress,
        @Email String companyEmail,
        @Min(0) Integer appointmentBufferTime
) { }
