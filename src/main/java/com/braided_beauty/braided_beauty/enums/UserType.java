package com.braided_beauty.braided_beauty.enums;

import java.util.Set;

public enum UserType {
    GUEST,
    MEMBER,
    ADMIN;

    // Map enum to Spring Security role strings
    public static Set<String> roleStringsFor(UserType type){
        return switch (type) {
            case ADMIN -> Set.of("ROLE_ADMIN");
            case MEMBER -> Set.of("ROLE_MEMBER");
            case GUEST -> Set.of("ROLE_GUEST");
        };
    }
}
