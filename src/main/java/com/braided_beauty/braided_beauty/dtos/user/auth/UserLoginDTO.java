package com.braided_beauty.braided_beauty.dtos.user.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserLoginDTO {
    private String oAuthProvider;
    private String token;
}
