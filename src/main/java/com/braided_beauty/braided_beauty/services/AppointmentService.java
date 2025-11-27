package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.config.SchedulingConfig;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentSummaryDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.AddOn;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@AllArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceRepository serviceRepository;
    private final AddOnService addOnService;
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

        Appointment appointment = appointmentDtoMapper.toEntity(appointmentRequestDTO);

        appointment.setService(service);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setDepositAmount(service.getDepositAmount());
        appointment.setPaymentStatus(PaymentStatus.PENDING);

        if (appointmentRequestDTO.getAddOnIds() != null && !appointmentRequestDTO.getAddOnIds().isEmpty()) {
            List<AddOn> addOns = addOnService.getAddOnIds(appointmentRequestDTO.getAddOnIds());
            appointment.setAddOns(addOns);
        }

        // Save the appointment so it gets an ID (needed for Stripe metadata)
        Appointment saved = appointmentRepository.save(appointment);
        Session session = paymentService.createDepositCheckoutSession(saved, "", "");

        // Save the Stripe session ID for future webhook/refund handling
        appointment.setStripeSessionId(session.getId());
        appointmentRepository.save(saved);

        service.setTimesBooked(+1);
        serviceRepository.save(service);

        return appointmentDtoMapper.toDTO(saved);
    }

    public AppointmentResponseDTO cancelAppointment(CancelAppointmentDTO dto) throws StripeException {
        UUID appointmentId = dto.getAppointmentId();
        UUID userId = dto.getUserId();
        String reason = dto.getCancelReason();

        Appointment canceledAppointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No appointment found."));

        ServiceModel service = canceledAppointment.getService();

        if (!canceledAppointment.getUser().getId().equals(userId)){
            throw new UnauthorizedException("You can't cancel someone else's appointment.");
        }

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.CANCELED){
            throw new ConflictException("Appointment already canceled.");
        }

        if (canceledAppointment.getPaymentStatus() == PaymentStatus.PAID_DEPOSIT ||
            canceledAppointment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            paymentService.updateRefundPayment(canceledAppointment);
            canceledAppointment.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        canceledAppointment.setAppointmentStatus(AppointmentStatus.CANCELED);
        canceledAppointment.setCancelReason(reason);
        canceledAppointment.setUpdatedAt(LocalDateTime.now());

        appointmentRepository.save(canceledAppointment);

        service.setTimesBooked(-1);
        serviceRepository.save(service);

        return appointmentDtoMapper.toDTO(canceledAppointment);
    }

    public AppointmentResponseDTO completeAppointment(UUID appointmentId) throws StripeException{
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));

        appointment.setAppointmentStatus(AppointmentStatus.PAYMENT_IN_PROGRESS);
        appointmentRepository.save(appointment);

        Session session = paymentService.createFinalPaymentSession(appointment, "", "", appointment.getTipAmount());

       appointment.setStripeSessionId(session.getId());
       appointmentRepository.save(appointment);

       return appointmentDtoMapper.toDTO(appointment);
    }

    public AppointmentResponseDTO getAppointmentById(UUID appointmentId){
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        log.info("Retrieved service with ID: {}", appointment.getId());
        return appointmentDtoMapper.toDTO(appointment);
    }

    public List<AppointmentResponseDTO> getAllAppointmentsByDate(LocalDate date){
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository
                .findAllByAppointmentTimeBetweenOrderByAppointmentTimeAsc(start, end);

        return appointments.stream()
                .map(appointmentDtoMapper::toDTO)
                .toList();
    }

    public Optional<AppointmentSummaryDTO> getNextAppointment(UUID userId) {
        LocalDateTime now = LocalDateTime.now();

        List<AppointmentStatus> statuses = List.of(
                AppointmentStatus.BOOKED,
                AppointmentStatus.CONFIRMED
        );

        return appointmentRepository.findFirstByUserIdAndAppointmentTimeAfterAndAppointmentStatusInOrderByAppointmentTimeAsc(userId, now, statuses)
                .map(appointmentDtoMapper::toSummaryDTO);
    }

    public Page<AppointmentSummaryDTO> getPreviousAppointments(UUID userId, Pageable pageable){
        LocalDateTime now = LocalDateTime.now();

        List<AppointmentStatus> statuses = List.of(
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELED
        );

        return appointmentRepository
                .findUserByIdAndAppointmentTimeBeforeAndAppointmentStatusIn(userId, now, statuses, pageable)
                .map(appointmentDtoMapper::toSummaryDTO);
    }
}
