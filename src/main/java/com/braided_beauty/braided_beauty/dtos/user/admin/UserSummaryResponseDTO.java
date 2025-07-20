package com.braided_beauty.braided_beauty.dtos.user.admin;

import com.braided_beauty.braided_beauty.enums.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserSummaryResponseDTO {
    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final String email;
    @NotNull
    private final UserType userType;
    @NotNull
    private final LocalDateTime createdAt;
    private final Integer loyaltyPoints;

}
