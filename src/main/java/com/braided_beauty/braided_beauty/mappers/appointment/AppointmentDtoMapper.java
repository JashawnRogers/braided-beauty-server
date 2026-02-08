package com.braided_beauty.braided_beauty.mappers.appointment;

import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class AppointmentDtoMapper {
    private final ServiceDtoMapper serviceDtoMapper;

    public Appointment toEntity(AppointmentRequestDTO dto){
        return Appointment.builder()
                .appointmentTime(dto.getAppointmentTime())
                .note(dto.getNote())
                .stripePaymentId(dto.getStripePaymentId())
                .build();
    }

    public AppointmentResponseDTO toDTO(Appointment appointment){
        var service = serviceDtoMapper.toDto(appointment.getService());
        String email = appointment.getUser() != null ? appointment.getUser().getEmail() : appointment.getGuestEmail();
        String guestCancelToken = appointment.getUser() == null ? appointment.getGuestCancelToken() : null;

        return AppointmentResponseDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .createdAt(appointment.getCreatedAt())
                .service(service)
                .depositAmount(appointment.getDepositAmount())
                .paymentStatus(appointment.getPaymentStatus())
                .stripePaymentId(appointment.getStripePaymentId())
                .pointsEarned(service.getPointsEarned())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .addOns(appointment.getAddOns())
                .email(email)
                .guestCancelToken(guestCancelToken)
                .build();
    }

    public AppointmentSummaryDTO toSummaryDTO(Appointment appointment) {

        return AppointmentSummaryDTO.builder()
                .id(appointment.getId())
                .serviceName(appointment.getService().getName())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .build();
    }

    public AdminAppointmentSummaryDTO toAdminSummaryDTO(Appointment appointment) {
        // --- Service basics ---
        ServiceModel service = appointment.getService();
        UUID serviceId = service != null ? service.getId() : null;
        String serviceName = service != null ? service.getName() : null;

        BigDecimal servicePrice = Optional.ofNullable(service)
                .map(ServiceModel::getPrice)
                .orElse(BigDecimal.ZERO);

        // --- Tip / Discount / Deposit (safe defaults) ---
        BigDecimal tip = Optional.ofNullable(appointment.getTipAmount()).orElse(BigDecimal.ZERO);

        BigDecimal discount = Optional.ofNullable(appointment.getDiscountAmount()).orElse(BigDecimal.ZERO);
        BigDecimal safeDiscount = discount.compareTo(BigDecimal.ZERO) > 0 ? discount : BigDecimal.ZERO;

        BigDecimal depositAmount = Optional.ofNullable(appointment.getDepositAmount()).orElse(BigDecimal.ZERO);

        // --- Add-ons ---
        List<AddOn> addOns = Optional.ofNullable(appointment.getAddOns()).orElseGet(List::of);

        List<UUID> addOnIds = addOns.stream()
                .map(AddOn::getId)
                .filter(Objects::nonNull)
                .toList();

        BigDecimal totalPriceOfAddOns = addOns.stream()
                .map(AddOn::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- Recomputed total for admin display ---
        BigDecimal subtotal = servicePrice.add(totalPriceOfAddOns);

        BigDecimal totalAmount = subtotal
                .subtract(safeDiscount)
                .subtract(depositAmount)
                .add(tip)
                .setScale(2, RoundingMode.HALF_UP);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // --- Customer info ---
        User user = appointment.getUser();

        String customerName =
                (user != null && user.getName() != null && !user.getName().isBlank())
                        ? user.getName()
                        : "Guest";

        String customerEmail =
                (appointment.getGuestEmail() != null && !appointment.getGuestEmail().isBlank())
                        ? appointment.getGuestEmail()
                        : (user != null ? user.getEmail() : null);

        return AdminAppointmentSummaryDTO.builder()
                .id(appointment.getId())
                .appointmentTime(appointment.getAppointmentTime())
                .appointmentStatus(appointment.getAppointmentStatus())
                .serviceId(serviceId)
                .serviceName(serviceName)
                .addOnIds(addOnIds)
                .paymentStatus(appointment.getPaymentStatus())
                .remainingBalance(appointment.getRemainingBalance())
                .totalAmount(totalAmount)
                .customerName(customerName)
                .customerEmail(customerEmail)
                .tipAmount(appointment.getTipAmount())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .note(appointment.getNote())
                .discountAmount(discount.setScale(2, RoundingMode.HALF_UP))
                .discountPercent(Optional.ofNullable(appointment.getDiscountPercent()).orElse(0))
                .build();
    }
}
