package com.braided_beauty.braided_beauty.mappers.user.admin;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.*;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.Service;
import com.braided_beauty.braided_beauty.models.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class UserAdminMapper {

    public Appointment toEntity(UserAdminAppointmentsRequestDTO dto,
                                                         Service service,
                                                         User user){
        return Appointment.builder()
                .id(dto.getAppointmentId())
                .user(user)
                .service(service)
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

    public UserAdminAppointmentResponseDTO toUserAdminAppointmentResponseDTO(User user,
                                                                             Service service,
                                                                             Appointment appointment){
        return UserAdminAppointmentResponseDTO.builder()
                .id(user.getId())
                .serviceId(service.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .notes(appointment.getNote())
                .build();
    }

    public UserAdminViewDTO toUserAdminViewDTO(UserAdminViewDTO dto,
                                               User user,
                                               AppointmentResponseDTO appointment,
                                               LoyaltyRecordResponseDTO loyaltyRecord){
        return UserAdminViewDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .phoneNumber(user.getPhoneNumber())
                .appointments(user.getAppointments()
                        .stream()
                        .map(appt -> appointment)
                        .collect(Collectors.toList())
                )
                .loyaltyRecord(loyaltyRecord)
                .build();
    }

    public UserSummaryResponseDTO toUserSummaryResponseDTO(User user, LoyaltyRecord loyaltyRecord){
        return UserSummaryResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .userType(user.getUserType())
                .createdAt(user.getCreatedAt())
                .loyaltyPoints(loyaltyRecord.getPoints())
                .build();
    }
}
