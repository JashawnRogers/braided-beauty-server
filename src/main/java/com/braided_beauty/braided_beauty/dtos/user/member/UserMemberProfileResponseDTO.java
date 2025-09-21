package com.braided_beauty.braided_beauty.dtos.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@Getter
@Setter
public class UserMemberProfileResponseDTO {
    @NotNull
    private final UUID id;
    @NotNull
    private final String name;
    @NotNull
    private final String email;
    private final String phoneNumber;
    private final List<AppointmentResponseDTO> appointments;
    private final LoyaltyRecordResponseDTO loyaltyRecord;
}
