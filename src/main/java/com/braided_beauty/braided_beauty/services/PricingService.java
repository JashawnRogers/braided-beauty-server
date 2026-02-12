package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.PromoCode;
import com.braided_beauty.braided_beauty.records.PricingBreakdown;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;


@Service
@AllArgsConstructor
public class PricingService {

    // REMINDER: pull DEPOSIT_RATE from BusinessSettings later
    private static final BigDecimal DEPOSIT_RATE = new BigDecimal("0.25");
    private static final BigDecimal DIVISOR = new BigDecimal("100");

    public PricingBreakdown calculate(Appointment appointment) {
        Objects.requireNonNull(appointment, "Appointment cannot be null");

        BigDecimal subtotal = money(computeSubtotal(appointment));
        BigDecimal deposit = money(subtotal.multiply(DEPOSIT_RATE));
        BigDecimal remainingBalance = money(subtotal.subtract(deposit));

        PromoCode promo = appointment.getPromoCode();
        BigDecimal discountAmount = money(computePromoDiscount(promo, remainingBalance));

        BigDecimal remainingDue = money(remainingBalance.subtract(discountAmount));
        if (remainingDue.compareTo(BigDecimal.ZERO) < 0) remainingDue = money(BigDecimal.ZERO);

        BigDecimal tip = money(Objects.requireNonNullElse(appointment.getTipAmount(), BigDecimal.ZERO));
        BigDecimal finalDue = money(remainingDue.add(tip));
        if (finalDue.compareTo(BigDecimal.ZERO) < 0) finalDue = money(BigDecimal.ZERO);

        return new PricingBreakdown(
                subtotal,
                deposit,
                remainingBalance,
                discountAmount,
                remainingDue,
                tip,
                finalDue
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
}
