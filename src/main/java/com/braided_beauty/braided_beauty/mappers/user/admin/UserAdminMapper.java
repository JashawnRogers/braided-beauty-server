package com.braided_beauty.braided_beauty.mappers.user.admin;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.*;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class UserAdminMapper {
    private final LoyaltyRecordDtoMapper loyaltyRecordDtoMapper;

    public Appointment toEntity(UserAdminAppointmentsRequestDTO dto,
                                                         ServiceModel serviceModel,
                                                         User user){
        return Appointment.builder()
                .id(dto.getAppointmentId())
                .user(user)
                .service(serviceModel)
                .appointmentTime(dto.getAppointmentTime())
                .appointmentStatus(dto.getAppointmentStatus())
                .note(dto.getNotes())
                .build();
    }

    public User toEntity(UserAdminRoleUpdateDTO dto){
        return User.builder()
                .userType(dto.getUserType())
                .build();
    }

    public UserAdminAppointmentResponseDTO toAppointmentDTO(User user,
                                                                             ServiceModel serviceModel,
                                                                             Appointment appointment){
        return UserAdminAppointmentResponseDTO.builder()
                .id(user.getId())
                .serviceId(serviceModel.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .notes(appointment.getNote())
                .build();
    }

    public UserSummaryResponseDTO toSummaryDTO(User user){
        return UserSummaryResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .loyaltyPoints(user.getLoyaltyRecord().getPoints())
                .build();
    }
}
