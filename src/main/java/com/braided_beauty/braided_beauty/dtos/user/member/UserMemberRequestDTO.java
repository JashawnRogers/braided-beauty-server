package com.braided_beauty.braided_beauty.dtos.user.member;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.utils.PhoneConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.websocket.OnMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserMemberRequestDTO {
    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final String email;
    @PhoneConstraint
    private final String phoneNumber;
    @NotNull(message = "User must have either role (Member, Guest, Admin)")
    private final UserType userType;
    @NotNull
    private final LoyaltyRecord loyaltyRecord;
}
