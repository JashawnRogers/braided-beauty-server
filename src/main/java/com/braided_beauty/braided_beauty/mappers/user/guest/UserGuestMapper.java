package com.braided_beauty.braided_beauty.mappers.user.guest;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.guest.UserGuestRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.guest.UserGuestResponseDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserGuestMapper {

    public User toEntity(UserGuestRequestDTO dto){
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .build();
    }

    public UserGuestResponseDTO toDTO(User user, AppointmentResponseDTO appointment){
        return UserGuestResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .userType(UserType.GUEST)
                .createdAt(appointment.getCreatedAt())
                .appointment(appointment)
                .build();
    }
}
