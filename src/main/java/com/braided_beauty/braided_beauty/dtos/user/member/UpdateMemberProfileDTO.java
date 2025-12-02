package com.braided_beauty.braided_beauty.dtos.user.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UpdateMemberProfileDTO {
    @NotBlank
    @Size(max = 100)
    private final String name;

    @Size(max = 20)
    private final String phoneNumber;
}
