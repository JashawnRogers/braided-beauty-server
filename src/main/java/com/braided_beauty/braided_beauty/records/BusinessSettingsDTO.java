package com.braided_beauty.braided_beauty.records;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.math.BigDecimal;


@Builder
public record BusinessSettingsDTO(
        String companyPhoneNumber,
        String companyAddress,
        @Email String companyEmail,
        @Min(0) Integer appointmentBufferTime,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal discountPercentage
        ) { }
