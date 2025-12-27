package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AdminAppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.CheckoutLinkResponse;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AddOnRepository;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AdminAppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceRepository serviceRepository;
    private final AddOnRepository addOnRepository;
    private final FrontendProps frontendProps;
    private final PaymentService paymentService;

    @Transactional
    public AdminAppointmentSummaryDTO adminCancelAppointment(AdminAppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(dto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with ID: " + dto.getAppointmentId()));

        if (appointment.getAppointmentStatus() == AppointmentStatus.CANCELED) {
            throw new ConflictException("Appointment already canceled");
        }

        if (dto.getCancelReason() != null && !dto.getCancelReason().isBlank()) {
            appointment.setCancelReason(dto.getCancelReason().trim());
        }

        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            appointment.setNote(dto.getNote().trim());
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        appointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(appointment);
        return appointmentDtoMapper.toAdminSummaryDTO(appointment);
    }

    @Transactional
    public AdminAppointmentSummaryDTO adminCompleteAppointmentCash(AdminAppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(dto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with ID: " + dto.getAppointmentId()));

        if (appointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Appointment has already been completed");
        }

        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            appointment.setNote(dto.getNote().trim());
        }

        if (dto.getTipAmount() != null) {
            appointment.setTipAmount(dto.getTipAmount());
        }

        appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        appointment.setPaymentStatus(PaymentStatus.PAID_IN_FULL);
        appointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(appointment);
        return appointmentDtoMapper.toAdminSummaryDTO(appointment);
    }

    @Transactional
    public AdminAppointmentSummaryDTO adminUpdateAppointment(AdminAppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(dto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with ID: " + dto.getAppointmentId()));

        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            appointment.setNote(dto.getNote().trim());
        }

        if (dto.getAppointmentStatus() == AppointmentStatus.CANCELED) {
            if (dto.getCancelReason() != null && !dto.getCancelReason().isBlank()) {
                appointment.setCancelReason(dto.getCancelReason().trim());
            }
            appointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        }

        if (dto.getServiceId() != null) {
            ServiceModel service = serviceRepository.findById(dto.getServiceId())
                            .orElseThrow(() -> new NotFoundException("Service not found"));
            appointment.setService(service);
            appointment.setAddOns(List.of());
        }

        if (dto.getAddOnIds() != null && !dto.getAddOnIds().isEmpty()) {
            List<AddOn> addOns = addOnRepository.findAllById(dto.getAddOnIds());
            if (addOns.size() != dto.getAddOnIds().size()) {
                throw new NotFoundException("One or more add-ons not found");
            }
            appointment.setAddOns(addOns);
        }

        if (dto.getTipAmount() != null) {
            appointment.setTipAmount(dto.getTipAmount());
        }

        appointmentRepository.save(appointment);
        return appointmentDtoMapper.toAdminSummaryDTO(appointment);
    }

    @Transactional
    public CheckoutLinkResponse adminCreateFinalPaymentViaQR(UUID appointmentId, BigDecimal tipAmount) throws StripeException {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));

        String successUrl = frontendProps.baseUrl() + "/book/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendProps.baseUrl() + "/book/cancel?appointmentId=" + appointment.getId();

        Session session = paymentService.createFinalPaymentSession(appointment, successUrl, cancelUrl, tipAmount);

        return new CheckoutLinkResponse(session.getUrl(), appointment.getId());
    }
}
