package com.braided_beauty.braided_beauty.dtos.user.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserJwtDTO {
    private final String token;
}
