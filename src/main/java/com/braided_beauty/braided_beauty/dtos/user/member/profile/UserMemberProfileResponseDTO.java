package com.braided_beauty.braided_beauty.dtos.user.member.profile;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class UserMemberProfileRequestDTO {
    @NotNull
    private final String name;
    @NotNull
    private final UserType userType;

}
