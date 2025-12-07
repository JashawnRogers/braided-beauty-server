package com.braided_beauty.braided_beauty.dtos.user.global;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequestDTO {
    private String currentPassword;

    private String newPassword;

    private String confirmNewPassword;
}
