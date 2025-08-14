package com.braided_beauty.braided_beauty.records;

import java.util.Set;
import java.util.UUID;

public record AppUserPrincipal(
        UUID id,
        String email,
        String name,
        Set<String> roles
) {
}
