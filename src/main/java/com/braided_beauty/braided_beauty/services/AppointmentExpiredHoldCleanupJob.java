package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentExpiredHoldCleanupJob {
    private final AppointmentRepository appointmentRepository;
    private final PaymentService paymentService;

    @Scheduled(cron = "0 * * * * *")
    public void deleteExpiredHolds() {
        List<UUID> expiredAppointmentIds = appointmentRepository.findExpiredPendingHolds().stream()
                .filter(appointment -> appointment.getAppointmentStatus() == AppointmentStatus.PENDING_CONFIRMATION)
                .map(Appointment::getId)
                .toList();

        for (UUID appointmentId : expiredAppointmentIds) {
            try {
                paymentService.reconcileExpiredPendingAppointmentHold(appointmentId);
            } catch (StripeException ex) {
                log.error("Failed to reconcile expired hold for appointment {}: {}", appointmentId, ex.getMessage(), ex);
            }
        }
    }
}
