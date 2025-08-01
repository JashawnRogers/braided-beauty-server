package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.config.SchedulingConfig;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.CancelAppointmentDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.exceptions.UnauthorizedException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
@AllArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
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

        Appointment appointment = appointmentDtoMapper.toEntity(appointmentRequestDTO);
        appointment.setService(service);
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setDepositAmount(service.getDepositAmount());
        appointment.setPaymentStatus(PaymentStatus.PENDING);

        // Save the appointment so it gets an ID (needed for Stripe metadata)
        Appointment saved = appointmentRepository.save(appointment);

        String appointmentId = saved.getId().toString();
        String userId = saved.getUser() != null ? saved.getUser().getId().toString() : "guest";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://placeholder.com/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://placeholder.com/cancel")
                .setCustomerEmail(appointmentRequestDTO.getReceiptEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(service.getDepositAmount().longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(service.getName())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("appointmentId", appointmentId)
                        .putMetadata("userId", userId)
                                .build();

        Session session = Session.create(params);

        // Save the Stripe session ID for future webhook/refund handling
        appointment.setStripeSessionId(session.getId());
        appointmentRepository.save(saved);

        log.info("Created appointment with ID: {}, ", saved.getId());
        return appointmentDtoMapper.toDTO(saved);
    }

    public AppointmentResponseDTO cancelAppointment(CancelAppointmentDTO dto) throws StripeException {
        UUID appointmentId = dto.getAppointmentId();
        UUID userId = dto.getUserId();
        String reason = dto.getCancelReason();

        Appointment canceledAppointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("No appointment found."));
        if (!canceledAppointment.getUser().getId().equals(userId)){
            throw new UnauthorizedException("You can't cancel someone else's appointment.");
        }

        //Created so that I can have a service dto to return the appt response dto
        ServiceModel service = serviceRepository.findById(canceledAppointment.getService().getId())
                .orElseThrow(() -> new NotFoundException("Service not found."));

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.CANCELED){
            throw new ConflictException("Appointment already canceled");
        }

        if (canceledAppointment.getAppointmentStatus() == AppointmentStatus.COMPLETED) {
            throw new ConflictException("Can't cancel a previously completed appointment.");
        }

        if (canceledAppointment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            try{
                String sessionId = canceledAppointment.getStripeSessionId();
                if (sessionId == null){
                    throw new IllegalStateException("Stripe session ID is missing for appointment " + appointmentId);
                }
                Session session = Session.retrieve(sessionId);
                paymentService.issueRefund(session.getPaymentIntent());
                canceledAppointment.setPaymentStatus(PaymentStatus.REFUNDED);
                log.info("Refunding PaymentIntent: {}, Appointment: {}", session.getPaymentIntent(), appointmentId);
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

        return appointmentDtoMapper.toDTO(canceledAppointment);
    }

    public AppointmentResponseDTO completeAppointment(UUID appointmentId) throws StripeException{
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));

        BigDecimal remainingBalance = appointment.getService().getPrice().subtract(appointment.getDepositAmount());

        String userEmail = appointment.getUser().getEmail();
        String userId = appointment.getUser() != null ? appointment.getUser().getId().toString() : "guest";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://placeholder.com/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl("https://placeholder.com/cancel")
                .setCustomerEmail(userEmail)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(remainingBalance.longValue())
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Remaining Balance: " + appointment.getService().getName())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("appointmentId", appointment.getId().toString())
                .putMetadata("userId", userId)
                .build();

        Session session = Session.create(params);



        appointment.setAppointmentStatus(AppointmentStatus.PAYMENT_IN_PROGRESS);
        appointment.setStripePaymentId(session.getId());

       Appointment saved = appointmentRepository.save(appointment);

        log.info("Created Stripe session {} for remaining balance on appointment {}", session.getId(), appointmentId);
       return appointmentDtoMapper.toDTO(saved);
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
}
