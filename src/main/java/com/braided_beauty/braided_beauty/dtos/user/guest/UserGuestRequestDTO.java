package com.braided_beauty.braided_beauty.dtos.user.guest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
public class UserGuestRequestDTO {
    @NotBlank
    private final String name;
    @Email
    @NotBlank
    private final String email;
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private final String phoneNumber;

}
