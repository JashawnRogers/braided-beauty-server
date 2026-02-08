package com.braided_beauty.braided_beauty.records;

import java.util.UUID;

public record ServicePopularityRow(
        UUID serviceId,
        String name,
        long completedCount
        ) {
}
