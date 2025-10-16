package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.loyaltyRecord.LoyaltyRecordResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.admin.*;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberProfileResponseDTO;
import com.braided_beauty.braided_beauty.dtos.user.member.UserMemberRequestDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.loyaltyRecord.LoyaltyRecordDtoMapper;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.mappers.user.admin.UserAdminMapper;
import com.braided_beauty.braided_beauty.mappers.user.member.UserMemberDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;


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

    public Page<UserSummaryResponseDTO> getAllUsers(
            String search,
            LocalDateTime createdAtFrom,
            LocalDateTime createdAtTo,
            UserType userType,
            Pageable pageable
    ) {
        Specification<User> spec = (root, q, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("email")), "%" + search.toLowerCase() + "%")
            ));
        }

        if (createdAtFrom != null) {
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
        }
        if (createdAtTo != null) {
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo));
        }

        if (userType != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("userType"), userType));
        }

        return userRepository.findAll(spec, pageable)
                .map(userAdminMapper::toSummaryDTO);
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

        // New clients?
        // Returning clients?

        return UserAdminAnalyticsDTO.builder()
                .totalAppointmentsByMonth(appointmentsByMonth)
                .totalAppointmentsAllTime(totalAppointmentsAllTime)
                .uniqueClientsThisMonth(newUsersThisMonth)
                .mostPopularService(serviceDtoMapper.toMostPopularServiceDto(mostPopularService))
                .totalLoyaltyMembers(loyaltyMembers)
                .build();
    }

    public UserSummaryResponseDTO updateUserRole(UUID userId, UserType userType){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        user.setUserType(userType);
        return userAdminMapper.toSummaryDTO(user);
    }

    // Creates account in local db for first time OAuth login
    public User registerFromOauth(Map<String, Object> attributes){
        String email = (String) attributes.get("email");
        String name = (String) attributes.getOrDefault("name", email);
        String providerId = (String) attributes.getOrDefault("sub", null);

        LoyaltyRecord loyaltyRecord = new LoyaltyRecord();

        User newUser = User.builder()
                .email(email)
                .name(name)
                .oAuthSubject("GOOGLE")
                .oAuthProvider(providerId)
                .userType(UserType.MEMBER)
                .loyaltyRecord(loyaltyRecord)
                .build();

        return userRepository.save(newUser);
    }

    public UUID findUserIdByEmail(String email) {
        return userRepository.findUserIdByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }

    @Transactional // <-- Allows JPA to push pending changes to DB
    public UserMemberProfileResponseDTO updateMemberData(UserMemberRequestDTO dto) {
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("User does not exist"));
        userRepository.save(UserMemberDtoMapper.toEntity(user, dto));
        // Saving is not needed due to JPA managing the entity at transaction commit
        return userMemberDtoMapper.toDTO(user);
    }
}
