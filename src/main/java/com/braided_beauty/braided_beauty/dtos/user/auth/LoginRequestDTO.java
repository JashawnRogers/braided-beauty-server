package com.braided_beauty.braided_beauty.dtos.user.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDTO {

    @NotBlank(message = "Must provide an email to login.")
    public String email;
    @NotBlank(message = "Must provide a password to login.")
    public String password;
}
