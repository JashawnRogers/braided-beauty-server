package com.braided_beauty.braided_beauty.mappers.user.member;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.models.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserMemberMapper {

    public User toEntity(UserMemberRequestDTO dto){
        return User.builder()
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }

    public UserMemberProfileResponseDTO toUserMemberProfileResponseDTO(AppointmentResponseDTO appointment,
                                                                       User user,
                                                                       LoyaltyRecordResponseDTO loyaltyRecord){
        return UserMemberProfileResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .appointments(user.getAppointments()
                        .stream()
                        .map(appt -> appointment)
                        .collect(Collectors.toList())
                )
                .loyalty(loyaltyRecord)
                .build();
    }
}
