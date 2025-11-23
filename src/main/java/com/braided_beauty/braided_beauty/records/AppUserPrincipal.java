package com.braided_beauty.braided_beauty.records;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public record AppUserPrincipal(
        UUID id,
        String email,
        String name,
        Collection<? extends GrantedAuthority> authorities

) { }
