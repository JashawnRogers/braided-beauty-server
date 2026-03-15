package com.braided_beauty.braided_beauty.dtos.bootstrap;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BootstrapAdminRequestDTO {
    @NotBlank(message = "Bootstrap secret is required.")
    private String secret;
}
