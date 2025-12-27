package com.braided_beauty.braided_beauty.records;

import java.util.UUID;

public record CheckoutLinkResponse(String checkoutUrl, UUID appointmentId) {
}
