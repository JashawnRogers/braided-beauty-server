package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.PaymentMethod;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.models.BusinessSettings;
import com.braided_beauty.braided_beauty.models.Payment;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.FrontendProps;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import com.braided_beauty.braided_beauty.repositories.ServiceRepository;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private AppointmentConfirmationService appointmentConfirmationService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private EmailService emailService;
    @Mock private BusinessSettingsService businessSettingsService;
    @Mock private StripeGateway stripeGateway;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                appointmentRepository,
                serviceRepository,
                loyaltyService,
                appointmentConfirmationService,
                emailTemplateService,
                emailService,
                businessSettingsService,
                new FrontendProps("https://frontend.example"),
                stripeGateway
        );
    }

    @Test
    void updateRefundPayment_marksPaymentRefundedAfterGatewayCall() throws Exception {
        Appointment appointment = appointmentWithUser();
        Payment payment = Payment.builder()
                .appointment(appointment)
                .paymentType(PaymentType.FINAL)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .stripePaymentIntentId("pi_123")
                .build();

        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL))
                .thenReturn(Optional.of(payment));

        paymentService.updateRefundPayment(appointment);

        verify(stripeGateway).createRefund(any());
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void createDepositCheckoutSession_reusesExistingPendingSession() throws Exception {
        Appointment appointment = appointmentWithUser();
        appointment.setDepositAmount(new BigDecimal("25.00"));

        Payment existing = Payment.builder()
                .stripeSessionId("cs_existing")
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .paymentType(PaymentType.DEPOSIT)
                .build();
        Session existingSession = new Session();
        existingSession.setId("cs_existing");

        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT))
                .thenReturn(Optional.of(existing));
        when(stripeGateway.retrieveCheckoutSession("cs_existing")).thenReturn(existingSession);

        Session result = paymentService.createDepositCheckoutSession(
                appointment,
                "https://frontend.example/success",
                "https://frontend.example/cancel"
        );

        assertSame(existingSession, result);
        verify(stripeGateway).retrieveCheckoutSession("cs_existing");
        verify(stripeGateway, never()).createCheckoutSession(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createDepositCheckoutSession_createsStripeSessionAndPersistsPendingPayment() throws Exception {
        Appointment appointment = appointmentWithUser();
        appointment.setDepositAmount(new BigDecimal("25.00"));

        Session createdSession = new Session();
        createdSession.setId("cs_new");
        createdSession.setPaymentIntent("pi_new");

        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT))
                .thenReturn(Optional.empty());
        when(stripeGateway.createCheckoutSession(any())).thenReturn(createdSession);

        Session result = paymentService.createDepositCheckoutSession(
                appointment,
                "https://frontend.example/success",
                "https://frontend.example/cancel"
        );

        assertSame(createdSession, result);

        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripeGateway).createCheckoutSession(paramsCaptor.capture());
        SessionCreateParams params = paramsCaptor.getValue();
        assertEquals("https://frontend.example/success", params.getSuccessUrl());
        assertEquals("https://frontend.example/cancel", params.getCancelUrl());
        assertEquals("member@example.com", params.getCustomerEmail());
        assertEquals("deposit", params.getMetadata().get("paymentType"));
        assertEquals(1, params.getLineItems().size());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(new BigDecimal("25.00"), savedPayment.getAmount());
        assertEquals(PaymentStatus.PENDING_PAYMENT, savedPayment.getPaymentStatus());
        assertEquals(PaymentType.DEPOSIT, savedPayment.getPaymentType());
        assertEquals(PaymentMethod.CARD, savedPayment.getPaymentMethod());
        assertSame(appointment, savedPayment.getAppointment());
        assertSame(appointment.getUser(), savedPayment.getUser());
    }

    @Test
    void createFinalPaymentSession_rejectsChangedAmountWhenPendingSessionAlreadyExists() throws Exception {
        Appointment appointment = appointmentWithUser();
        appointment.setRemainingBalance(new BigDecimal("100.00"));
        appointment.setFeeTotal(new BigDecimal("5.00"));

        Payment existing = Payment.builder()
                .stripeSessionId("cs_existing_final")
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .paymentType(PaymentType.FINAL)
                .amount(new BigDecimal("100.00"))
                .build();

        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> paymentService.createFinalPaymentSession(
                appointment,
                "https://frontend.example/success",
                "https://frontend.example/cancel",
                new BigDecimal("10.00")
        ));

        verify(stripeGateway, never()).createCheckoutSession(any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createFinalPaymentSession_createsStripeSessionAndPersistsFullAmount() throws Exception {
        Appointment appointment = appointmentWithUser();
        appointment.setRemainingBalance(new BigDecimal("80.00"));
        appointment.setFeeTotal(new BigDecimal("5.00"));
        appointment.setDiscountAmount(new BigDecimal("10.00"));
        appointment.setPostDepositBalanceAtBooking(new BigDecimal("90.00"));
        appointment.setPromoCodeText("save10");

        Session createdSession = new Session();
        createdSession.setId("cs_final");
        createdSession.setPaymentIntent("pi_final");

        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.FINAL))
                .thenReturn(Optional.empty());
        when(stripeGateway.createCheckoutSession(any())).thenReturn(createdSession);

        Session result = paymentService.createFinalPaymentSession(
                appointment,
                "https://frontend.example/success",
                "https://frontend.example/cancel",
                new BigDecimal("15.00")
        );

        assertSame(createdSession, result);

        ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripeGateway).createCheckoutSession(paramsCaptor.capture());
        SessionCreateParams params = paramsCaptor.getValue();
        assertEquals("final", params.getMetadata().get("paymentType"));
        assertEquals("SAVE10", params.getPaymentIntentData().getMetadata().get("promoCode"));
        assertEquals("15.00", params.getPaymentIntentData().getMetadata().get("tipAmount"));
        assertEquals("5.00", params.getPaymentIntentData().getMetadata().get("feeAmount"));
        assertEquals(3, params.getLineItems().size());

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(new BigDecimal("100.00"), savedPayment.getAmount());
        assertEquals(new BigDecimal("15.00"), savedPayment.getTipAmount());
        assertEquals(new BigDecimal("5.00"), savedPayment.getFee());
        assertEquals(PaymentStatus.PENDING_PAYMENT, savedPayment.getPaymentStatus());
        assertEquals(PaymentType.FINAL, savedPayment.getPaymentType());
    }

    @Test
    void handleCheckoutSessionCompleted_confirmsPendingDepositAppointment() {
        Appointment appointment = appointmentWithUser();
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        appointment.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));
        appointment.setDepositAmount(new BigDecimal("25.00"));
        appointment.setPostDepositBalanceAtBooking(new BigDecimal("75.00"));
        appointment.setDiscountAmount(BigDecimal.ZERO);
        appointment.setSubtotalAtBooking(new BigDecimal("100.00"));

        Payment payment = Payment.builder()
                .appointment(appointment)
                .paymentType(PaymentType.DEPOSIT)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .stripeSessionId("cs_paid")
                .build();

        BusinessSettings businessSettings = new BusinessSettings();
        businessSettings.setCompanyPhoneNumber("6025551212");
        businessSettings.setCompanyAddress("123 Main St");
        businessSettings.setCompanyEmail("owner@example.com");

        Session session = new Session();
        session.setId("cs_paid");
        session.setPaymentIntent("pi_paid");
        session.setMetadata(Map.of(
                "appointmentId", appointment.getId().toString(),
                "paymentType", "deposit"
        ));

        when(paymentRepository.findByStripeSessionId("cs_paid")).thenReturn(Optional.of(payment));
        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(serviceRepository.save(any(ServiceModel.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(businessSettingsService.getOrCreate()).thenReturn(businessSettings);
        when(emailTemplateService.render(any(), any())).thenReturn("<html/>");

        paymentService.handleCheckoutSessionCompleted(session);

        assertEquals(AppointmentStatus.CONFIRMED, appointment.getAppointmentStatus());
        assertEquals(PaymentStatus.PAID_DEPOSIT, appointment.getPaymentStatus());
        assertEquals(new BigDecimal("75.00"), appointment.getRemainingBalance());
        assertEquals(null, appointment.getHoldExpiresAt());
        verify(paymentRepository).save(payment);
        verify(appointmentRepository).save(appointment);
        verify(serviceRepository).save(appointment.getService());
    }

    @Test
    void handleCheckoutSessionFailed_deletesPendingDepositHold() {
        Appointment appointment = appointmentWithUser();
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);

        Payment payment = Payment.builder()
                .appointment(appointment)
                .paymentType(PaymentType.DEPOSIT)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .stripeSessionId("cs_failed")
                .build();

        Session session = new Session();
        session.setId("cs_failed");
        session.setMetadata(Map.of(
                "appointmentId", appointment.getId().toString(),
                "paymentType", "deposit"
        ));

        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByStripeSessionId("cs_failed")).thenReturn(Optional.of(payment));

        paymentService.handleCheckoutSessionFailed(session);

        verify(paymentRepository).deleteByAppointment_Id(appointment.getId());
        verify(appointmentRepository).delete(appointment);
    }

    @Test
    void releasePendingAppointmentHold_expiresOpenSessionAndDeletesAppointment() throws Exception {
        Appointment appointment = appointmentWithUser();
        appointment.setAppointmentStatus(AppointmentStatus.PENDING_CONFIRMATION);
        appointment.setPaymentStatus(PaymentStatus.PENDING_PAYMENT);
        appointment.setStripeSessionId("cs_open");

        Payment payment = Payment.builder()
                .appointment(appointment)
                .paymentType(PaymentType.DEPOSIT)
                .paymentStatus(PaymentStatus.PENDING_PAYMENT)
                .stripeSessionId("cs_open")
                .build();

        Session session = new Session();
        session.setId("cs_open");
        session.setStatus("open");
        session.setPaymentStatus("unpaid");

        when(appointmentRepository.findByIdForUpdate(appointment.getId())).thenReturn(Optional.of(appointment));
        when(paymentRepository.findByAppointment_IdAndPaymentType(appointment.getId(), PaymentType.DEPOSIT))
                .thenReturn(Optional.of(payment));
        when(stripeGateway.retrieveCheckoutSession("cs_open")).thenReturn(session);

        paymentService.releasePendingAppointmentHold(appointment.getId());

        verify(stripeGateway).expireCheckoutSession("cs_open");
        verify(paymentRepository).deleteByAppointment_Id(appointment.getId());
        verify(appointmentRepository).delete(appointment);
    }

    private static Appointment appointmentWithUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("member@example.com");
        user.setName("Member");

        ServiceModel service = new ServiceModel();
        service.setName("Knotless Braids");
        service.setTimesBooked(0);

        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setUser(user);
        appointment.setService(service);
        appointment.setAddOns(new ArrayList<>());
        appointment.setFeeTotal(BigDecimal.ZERO.setScale(2));
        appointment.setAppointmentTime(LocalDateTime.of(2026, 5, 10, 9, 0));
        return appointment;
    }
}
