package com.braided_beauty.braided_beauty.mappers.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserDashboardDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.braided_beauty.braided_beauty.utils.PhoneNormalizer.toE164;

@Component
@AllArgsConstructor
public class UserMemberDtoMapper {
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final LoyaltyRecordDtoMapper loyaltyRecordDtoMapper;

    public static User toEntity(User target, UserMemberRequestDTO dto){
        // Mapper to entity should perform updates to prevent saving to entity with same state
        if (dto.getName() != null) target.setName(dto.getName());
        if (dto.getEmail() != null) target.setEmail(dto.getEmail());
        if (dto.getUserType() != null) target.setUserType(dto.getUserType());

        if (dto.getPhoneNumber() != null) {
            String normalized = toE164(dto.getPhoneNumber()).orElse(null);
            target.setPhoneNumber(normalized);
        }

        if (dto.getLoyaltyRecord() != null) {
            LoyaltyRecord loyaltyRecord = target.getLoyaltyRecord();
            if (loyaltyRecord == null) {
                loyaltyRecord = new LoyaltyRecord();
                loyaltyRecord.setUser(target);
                target.setLoyaltyRecord(loyaltyRecord);
            }
            if (dto.getLoyaltyRecord().getPoints() != null) {
                loyaltyRecord.setPoints(dto.getLoyaltyRecord().getPoints());
            }
            if (dto.getLoyaltyRecord().getRedeemedPoints() != null) {
                loyaltyRecord.setRedeemedPoints(dto.getLoyaltyRecord().getRedeemedPoints());
            }
            loyaltyRecord.setUpdatedAt(LocalDateTime.now());
        }

        return target;
    }

    public UserMemberProfileResponseDTO toDTO(User user){
        return UserMemberProfileResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .userType(user.getUserType())
                .updatedAt(user.getUpdatedAt())
                .createdAt(user.getCreatedAt())
                .appointments(user.getAppointments()
                        .stream()
                        .map(appointmentDtoMapper::toDTO)
                        .collect(Collectors.toList())
                )
                .loyaltyRecord(loyaltyRecordDtoMapper.toDTO(user.getLoyaltyRecord()))
                .build();
    }

    public UserDashboardDTO toDashboardDTO(User user, AppointmentSummaryDTO aptDTO) {
        return UserDashboardDTO.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .loyaltyRecord(user.getLoyaltyRecord())
                .appointmentCount(user.getAppointments().size())
                .nextApt(aptDTO)
                .build();
    }
}
