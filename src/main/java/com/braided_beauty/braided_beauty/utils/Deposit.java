package com.braided_beauty.braided_beauty.utils;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public final class Deposit {
    private Deposit() {}

    public static BigDecimal getDepositAmount(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(.20));
    }
}
