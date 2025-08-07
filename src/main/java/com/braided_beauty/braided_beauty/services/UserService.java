package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.*;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.mappers.user.admin.UserAdminMapper;
import com.braided_beauty.braided_beauty.mappers.user.member.UserMemberDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;


@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceRepository serviceRepository;
    private final UserMemberDtoMapper userMemberDtoMapper;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceDtoMapper serviceDtoMapper;
    private final LoyaltyRecordDtoMapper loyaltyRecordDtoMapper;
    private final UserAdminMapper userAdminMapper;

    public UserMemberProfileResponseDTO getMemberProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        return userMemberDtoMapper.toDTO(user);
    }

    public List<AppointmentResponseDTO> getAppointmentHistory(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist."));

        List<Appointment> appointments = user.getAppointments();
        return appointments.stream()
                .map(appointmentDtoMapper::toDTO)
                .toList();
    }

    public LoyaltyRecordResponseDTO getLoyaltyPoints(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User does not exist."));
        return loyaltyRecordDtoMapper.toDTO(user.getLoyaltyRecord());
    }

    public List<UserSummaryResponseDTO> getAllUsers(){
        List<User> users = userRepository.findAll();

        return users.stream()
                .map(userAdminMapper::toSummaryDTO)
                .toList();
    }

    public UserAdminAnalyticsDTO getAnalytics(YearMonth yearMonth){
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        List<AppointmentResponseDTO> appointmentsByMonth = appointmentRepository.findAllByCreatedAtBetweenOrderByCreatedAtAsc(start,end)
                .stream()
                .map(appointmentDtoMapper::toDTO)
                .toList();

        int totalAppointmentsAllTime = appointmentRepository.findAll().size();
        List<UserSummaryResponseDTO> newUsersThisMonth = userRepository.findAllByCreatedAtBetweenOrderByCreatedAtAsc(start, end)
                .stream()
                .map(userAdminMapper::toSummaryDTO)
                .toList();

        ServiceModel mostPopularService = serviceRepository.findTopByOrderByTimesBookedDesc()
                .orElseThrow(() -> new NotFoundException("Service not found."));

        int loyaltyMembers = userRepository.findAllByUserType(UserType.MEMBER).size();

        return UserAdminAnalyticsDTO.builder()
                .totalAppointmentsByMonth(appointmentsByMonth)
                .totalAppointmentsAllTime(totalAppointmentsAllTime)
                .uniqueClientsThisMonth(newUsersThisMonth)
                .mostPopularService(serviceDtoMapper.toDto(mostPopularService))
                .totalLoyaltyMembers(loyaltyMembers)
                .build();
    }

    public UserSummaryResponseDTO updateUserRole(UUID userId, UserType userType){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        user.setUserType(userType);
        return userAdminMapper.toSummaryDTO(user);
    }
}
