package com.braided_beauty.braided_beauty.dtos.user.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserLoginDTO {
    @NotNull
    private String oAuthProvider;
    @NotNull
    private String token;
}
