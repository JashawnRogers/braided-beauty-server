package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.BookingPricingPreview;
import com.braided_beauty.braided_beauty.records.PricingBreakdown;
import com.braided_beauty.braided_beauty.records.PromoPreviewResult;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import com.braided_beauty.braided_beauty.repositories.PromoCodeRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


@Service
@AllArgsConstructor
public class PricingService {
    private final PromoCodeRepository promoCodeRepository;
    private final ServiceRepository serviceRepository;
    private final AddOnRepository addOnRepository;
    private final Clock clock;

    // REMINDER: pull DEPOSIT_RATE from BusinessSettings later
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.25");
    private static final BigDecimal DIVISOR = new BigDecimal("100");

    public PricingBreakdown calculate(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");

        BigDecimal subtotal = money(computeSubtotal(appointment));
        BigDecimal deposit = money(subtotal.multiply(DEPOSIT_RATE));
        BigDecimal postDepositBalance = money(subtotal.subtract(deposit));

        PromoCode promo = appointment.getPromoCode();
        BigDecimal promoDiscount = money(computePromoDiscount(promo, postDepositBalance));

        BigDecimal amountDueBeforeTip = money(postDepositBalance.subtract(promoDiscount));
        if (amountDueBeforeTip.compareTo(BigDecimal.ZERO) < 0) amountDueBeforeTip = money(BigDecimal.ZERO);

        BigDecimal tip = money(Objects.requireNonNullElse(appointment.getTipAmount(), BigDecimal.ZERO));
        BigDecimal amountDue = money(amountDueBeforeTip.add(tip));
        if (amountDue.compareTo(BigDecimal.ZERO) < 0) amountDue = money(BigDecimal.ZERO);

        return new PricingBreakdown(
                subtotal,
                deposit,
                postDepositBalance,
                promoDiscount,
                amountDueBeforeTip,
                tip,
                amountDue
        );
    }

    public BigDecimal computeSubtotal(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");

        BigDecimal servicePrice = BigDecimal.ZERO;
        if (appointment.getService() != null && appointment.getService().getPrice() != null) {
            servicePrice = appointment.getService().getPrice();
        }

        BigDecimal addOnsTotal = computeAddOnsTotal(appointment);

        return money(servicePrice.add(addOnsTotal));
    }

    private BigDecimal computeAddOnsTotal(Appointment appointment) {
        if (appointment.getAddOns() == null) return money(BigDecimal.ZERO);

        return money(appointment.getAddOns()
                .stream()
                .map(AddOn::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

    }

    public BookingPricingPreview previewBookingPricing(UUID serviceId, List<UUID> addOnIds, String promoText) {
        if (serviceId == null) {
            throw new IllegalArgumentException("Service ID is required.");
        }

        ServiceModel service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service not found."));

        BigDecimal servicePrice = money(service.getPrice());

        BigDecimal addOnsTotal = BigDecimal.ZERO;
        if (addOnIds != null && !addOnIds.isEmpty()) {
            List<AddOn> addOns = addOnRepository.findAllById(addOnIds);
            addOnsTotal = money(addOns.stream()
                    .map(AddOn::getPrice)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        BigDecimal subtotal = money(servicePrice.add(addOnsTotal));
        BigDecimal deposit = money(subtotal.multiply(DEPOSIT_RATE));
        BigDecimal postDepositBalance = money(subtotal.subtract(deposit));

        PromoPreviewResult promo = validatePromoForPreview(promoText, postDepositBalance);

        return new BookingPricingPreview(subtotal, deposit, postDepositBalance, promo);
    }

    /**
     * Promo discount is applied ONLY to the remaining balance portion (post-deposit).
     * Deposit is unchanged.
     */
    private BigDecimal computePromoDiscount(PromoCode promo, BigDecimal remainingBalance) {
        if (promo == null) return money(BigDecimal.ZERO);
        if (promo.getDiscountType() == null || promo.getValue() == null) return money(BigDecimal.ZERO);

        BigDecimal promoValue = promo.getValue();
        if (promoValue.compareTo(BigDecimal.ZERO) <= 0) return money(BigDecimal.ZERO);

        return switch (promo.getDiscountType()) {
            case AMOUNT -> money(promoValue.min(remainingBalance));
            case PERCENT -> {
                BigDecimal rate = promoValue.divide(DIVISOR, 6, RoundingMode.HALF_UP);
                yield money(remainingBalance.multiply(rate));
            }
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private PromoPreviewResult validatePromoForPreview(String promoText, BigDecimal postDepositBalance) {
        if (promoText == null) {
            return invalid("Enter a promo code if applicable.");
        }
        String code = promoText.trim().toUpperCase(Locale.ROOT);

        BigDecimal remaining = money(postDepositBalance);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            return invalid("Invalid remaining balance.");
        }

        PromoCode promoCode = promoCodeRepository.findByCodeIgnoreCase(code).orElse(null);
        if (promoCode == null) {
            return invalid("Invalid promo code.");
        }

        if (!promoCode.isActive()) {
            return invalid("Promo code is inactive.");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (promoCode.getStartsAt() != null && now.isBefore(promoCode.getStartsAt())) {
            return invalid("Promo code has not started yet.");
        }
        if (promoCode.getEndsAt() != null && now.isAfter(promoCode.getEndsAt())) {
            return invalid("Promo code has expired.");
        }

        Integer max = promoCode.getMaxRedemptions();
        int used = Objects.requireNonNullElse(promoCode.getTimesRedeemed(), 0);
        if (max != null && used >= max) {
            return invalid("Promo code has reached its redemption limit.");
        }

        BigDecimal discount = money(computePromoDiscount(promoCode, remaining));
        BigDecimal remainingAfterPromo = money(remaining.subtract(discount));
        if (remainingAfterPromo.compareTo(BigDecimal.ZERO) < 0) remainingAfterPromo = money(BigDecimal.ZERO);

        String label = formatPromoLabel(promoCode);

        return valid(promoCode.getId(), label, discount, remainingAfterPromo);
    }

    private static PromoPreviewResult invalid(String message) {
        return new PromoPreviewResult(false,
                message,
                null,
                null,
                null,
                null
        );
    }

    private static PromoPreviewResult valid(UUID promoId, String promoLabel, BigDecimal discountAmount, BigDecimal remainingAfterPromo) {
        return new PromoPreviewResult(
                true,
                null,
                promoId,
                promoLabel,
                discountAmount,
                remainingAfterPromo
        );
    }

    private static String formatPromoLabel(PromoCode promoCode) {
        if (promoCode == null || promoCode.getDiscountType() == null || promoCode.getValue() == null) return null;

        return switch (promoCode.getDiscountType()) {
            case PERCENT -> promoCode.getValue().stripTrailingZeros().toPlainString() + "% off";
            case AMOUNT -> "$" + promoCode.getValue().setScale(2, RoundingMode.HALF_UP).toPlainString() + " off";
        };
    }
}
