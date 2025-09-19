package com.braided_beauty.braided_beauty.mappers.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.braided_beauty.braided_beauty.utils.PhoneNormalizer.toE164;

@Component
@AllArgsConstructor
public class UserMemberDtoMapper {
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceDtoMapper serviceDtoMapper;
    private final LoyaltyRecordDtoMapper loyaltyRecordDtoMapper;

    public static User toEntity(UserMemberRequestDTO dto){
        return User.builder()
                .id(dto.getId())
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(toE164(dto.getPhoneNumber()))
                .userType(dto.getUserType())
                .loyaltyRecord(dto.getLoyaltyRecord())
                .build();
    }

    public UserMemberProfileResponseDTO toDTO(User user){
        return UserMemberProfileResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .appointments(user.getAppointments()
                        .stream()
                        .map(appt -> appointmentDtoMapper.toDTO(appt))
                        .collect(Collectors.toList())
                )
                .loyalty(loyaltyRecordDtoMapper.toDTO(user.getLoyaltyRecord()))
                .build();
    }
}
