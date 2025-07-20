package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.enums.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserAdminRoleUpdateDTO {
    @NotNull
    private UserType userType;
}
