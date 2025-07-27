package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.config.SchedulingConfig;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceDtoMapper serviceDtoMapper;
    private final ServiceRepository serviceRepository;
    private final PaymentService paymentService;
    private final SchedulingConfig schedulingConfig;
    private final static Logger log = LoggerFactory.getLogger(AppointmentService.class);

    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO appointmentRequestDTO) throws StripeException {
        ServiceModel service = serviceRepository.findById(appointmentRequestDTO.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));

        int bufferMinutes = schedulingConfig.getBufferMinutes();
        LocalDateTime start = appointmentRequestDTO.getAppointmentTime();
        LocalDateTime end = start.plusMinutes(service.getDurationMinutes() + bufferMinutes);
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(start, end, bufferMinutes);
        if (!conflicts.isEmpty()){
            throw new ConflictException("This time overlaps with another appointment.");
        }

        ServiceResponseDTO serviceResponseDTO = serviceDtoMapper.toDTO(service);

        PaymentIntentRequestDTO paymentDto = PaymentIntentRequestDTO.builder()
                .amount(service.getDepositAmount())
                .currency("usd")
                .receiptEmail(appointmentRequestDTO.getReceiptEmail())
                .build();
        PaymentIntentResponseDTO stripeResponse = paymentService.createPaymentIntent(paymentDto);

        Appointment appointment = appointmentDtoMapper.toEntity(appointmentRequestDTO);

        // Manually overriding mapped values to appointment because they are dependent on stripe and service
        // rather than the client
        appointment.setService(service);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setDepositAmount(service.getDepositAmount());
        appointment.setPaymentStatus(PaymentStatus.PENDING);
        appointment.setStripePaymentId(stripeResponse.getPaymentIntentId());

        Appointment saved = appointmentRepository.save(appointment);

        log.info("Created appointment with ID: {}, ", saved.getId());
        return appointmentDtoMapper.toDTO(saved, serviceResponseDTO);
    }

    public void cancelAppointment(CancelAppointmentDTO dto) throws StripeException {
        UUID appointmentId = dto.getAppointmentId();
        UUID userId = dto.getUserId();
        String reason = dto.getCancelReason();

        Appointment canceledAppointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No appointment found."));
        if (!canceledAppointment.getUser().getId().equals(userId)){
            throw new UnauthorizedException("You can't cancel someone else's appointment.");
        }

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.CANCELED){
            throw new ConflictException("Appointment already canceled");
        }

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Can't cancel a previously completed appointment");
        }

        if (canceledAppointment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            try{
                paymentService.issueRefund(canceledAppointment.getStripePaymentId());
                canceledAppointment.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refund issued for amount: {} ", canceledAppointment.getService().getPrice());
            } catch (StripeException e) {
                log.error("Stripe refund failed for appointment {}: {}", canceledAppointment.getId(), e.getMessage());
                throw e;
            }
        }

        canceledAppointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        canceledAppointment.setCancelReason(reason);
        canceledAppointment.setUpdatedAt(LocalDateTime.now());

        log.info("Canceled appointment with ID: {}, Reason: {}", appointmentId, reason);
        appointmentRepository.save(canceledAppointment);
    }
}
