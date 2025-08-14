package com.braided_beauty.braided_beauty.dtos.user.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDTO {
    @NotBlank(message = "Must provide email to register")
    public String email;

    @NotBlank(message = "Must provide password to login")
    public String password;

    public String name;

    public String phoneNumber;
}
