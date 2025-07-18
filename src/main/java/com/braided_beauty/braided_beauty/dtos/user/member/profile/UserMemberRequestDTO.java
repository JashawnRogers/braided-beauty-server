package com.braided_beauty.braided_beauty.dtos.user.member.profile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class UserMemberRequestDTO {
    @NotNull
    private final String name;
    @NotNull
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private final String phoneNumber;
}
