package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.appointment.AppointmentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentRequestDTO;
import com.braided_beauty.braided_beauty.dtos.payment.PaymentIntentResponseDTO;
import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.mappers.appointment.AppointmentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.payment.PaymentDtoMapper;
import com.braided_beauty.braided_beauty.mappers.service.ServiceDtoMapper;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.exception.StripeException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDtoMapper appointmentDtoMapper;
    private final ServiceDtoMapper serviceDtoMapper;
    private final ServiceRepository serviceRepository;
    private final PaymentService paymentService;

    public AppointmentResponseDTO createAppointment(AppointmentRequestDTO appointmentRequestDTO) throws StripeException {
        ServiceModel service = serviceRepository.findById(appointmentRequestDTO.getServiceId())
                .orElseThrow(() -> new NotFoundException("Service not found"));
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
        appointment.setDepositAmount(service.getDepositAmount());
        appointment.setPaymentStatus(PaymentStatus.PENDING);
        appointment.setStripePaymentId(stripeResponse.getPaymentIntentId());

        Appointment saved = appointmentRepository.save(appointment);

        return appointmentDtoMapper.toDTO(saved, serviceResponseDTO);
    }
}
