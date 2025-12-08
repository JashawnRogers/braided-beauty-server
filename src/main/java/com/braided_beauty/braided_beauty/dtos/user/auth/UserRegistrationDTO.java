package com.braided_beauty.braided_beauty.dtos.user.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegistrationDTO {
    @NotBlank(message = "Must provide email to register")
    @Email
    public String email;

    @NotBlank(message = "Must provide password to login")
    @Size(min=8, max=128)
    public String password;

    @NotBlank
    @Size(max=15)
    public String name;

    @Pattern(regexp="^\\+1\\d{10}$", message="Phone number must be in +1XXXXXXXXXX format")
    public String phoneNumber;
}
