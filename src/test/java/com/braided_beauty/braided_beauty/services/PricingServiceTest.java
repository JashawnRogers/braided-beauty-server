package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.DiscountType;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.BusinessSettings;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.BookingPricingPreview;
import com.braided_beauty.braided_beauty.records.PricingBreakdown;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import com.braided_beauty.braided_beauty.repositories.PromoCodeRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private AddOnRepository addOnRepository;
    @Mock
    private BusinessSettingsService businessSettingsService;

    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-03T12:00:00Z"), ZoneOffset.UTC);
        pricingService = new PricingService(
                promoCodeRepository,
                serviceRepository,
                addOnRepository,
                businessSettingsService,
                fixedClock
        );

        BusinessSettings settings = new BusinessSettings();
        settings.setDiscountPercentage(new BigDecimal("25"));
        when(businessSettingsService.getOrCreate()).thenReturn(settings);
    }

    @Test
    void calculate_appliesPromoOnlyToPostDepositBalanceAndAddsTip() {
        Appointment appointment = new Appointment();
        ServiceModel service = new ServiceModel();
        service.setPrice(new BigDecimal("100.00"));
        appointment.setService(service);

        AddOn addOn = new AddOn();
        addOn.setPrice(new BigDecimal("20.00"));
        appointment.setAddOns(List.of(addOn));

        PromoCode promoCode = PromoCode.builder()
                .discountType(DiscountType.PERCENT)
                .value(new BigDecimal("50"))
                .build();
        appointment.setPromoCode(promoCode);
        appointment.setTipAmount(new BigDecimal("10.00"));

        PricingBreakdown breakdown = pricingService.calculate(appointment);

        assertEquals(new BigDecimal("120.00"), breakdown.subtotal());
        assertEquals(new BigDecimal("30.00"), breakdown.deposit());
        assertEquals(new BigDecimal("90.00"), breakdown.postDepositBalance());
        assertEquals(new BigDecimal("45.00"), breakdown.promoDiscount());
        assertEquals(new BigDecimal("45.00"), breakdown.amountDueBeforeTip());
        assertEquals(new BigDecimal("10.00"), breakdown.tip());
        assertEquals(new BigDecimal("55.00"), breakdown.amountDuePlusTip());
    }

    @Test
    void calculate_treatsNullDiscountPercentageAsZero() {
        BusinessSettings settings = new BusinessSettings();
        settings.setDiscountPercentage(null);
        when(businessSettingsService.getOrCreate()).thenReturn(settings);

        Appointment appointment = new Appointment();
        ServiceModel service = new ServiceModel();
        service.setPrice(new BigDecimal("100.00"));
        appointment.setService(service);

        PricingBreakdown breakdown = assertDoesNotThrow(() -> pricingService.calculate(appointment));

        assertEquals(new BigDecimal("100.00"), breakdown.subtotal());
        assertEquals(new BigDecimal("0.00"), breakdown.deposit());
        assertEquals(new BigDecimal("100.00"), breakdown.postDepositBalance());
    }

    @Test
    void previewBookingPricing_rejectsExpiredPromoCode() {
        UUID serviceId = UUID.randomUUID();
        ServiceModel service = new ServiceModel();
        service.setPrice(new BigDecimal("100.00"));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        PromoCode promoCode = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("SAVE10")
                .active(true)
                .discountType(DiscountType.AMOUNT)
                .value(new BigDecimal("10.00"))
                .endsAt(LocalDateTime.of(2026, 4, 2, 12, 0))
                .build();
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(promoCode));

        BookingPricingPreview preview = pricingService.previewBookingPricing(serviceId, null, "save10");

        assertFalse(preview.promo().valid());
        assertEquals("Promo code has expired.", preview.promo().message());
    }

    @Test
    void previewBookingPricing_clampsAmountPromoToRemainingBalance() {
        UUID serviceId = UUID.randomUUID();
        ServiceModel service = new ServiceModel();
        service.setPrice(new BigDecimal("20.00"));
        when(serviceRepository.findById(serviceId)).thenReturn(Optional.of(service));

        PromoCode promoCode = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("SAVE50")
                .active(true)
                .discountType(DiscountType.AMOUNT)
                .value(new BigDecimal("50.00"))
                .timesRedeemed(0)
                .build();
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE50")).thenReturn(Optional.of(promoCode));

        BookingPricingPreview preview = pricingService.previewBookingPricing(serviceId, null, "SAVE50");

        assertTrue(preview.promo().valid());
        assertEquals(new BigDecimal("15.00"), preview.promo().discountAmount());
        assertEquals(new BigDecimal("0.00"), preview.promo().remainingAfterPromo());
    }
}
