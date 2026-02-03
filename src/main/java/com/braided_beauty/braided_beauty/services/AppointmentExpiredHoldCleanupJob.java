package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.models.Appointment;
import com.braided_beauty.braided_beauty.repositories.AppointmentRepository;
import com.braided_beauty.braided_beauty.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentExpiredHoldCleanupJob {
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 0 * * * *") // top of every hour
    @Transactional
    public void deleteExpiredHolds() {
        List<Appointment> expiredAppointments = appointmentRepository.findExpiredPendingHolds();

        for (Appointment appointment : expiredAppointments) {
            boolean depositPaid = paymentRepository.existsByAppointment_IdAndPaymentTypeAndPaymentStatus(
                    appointment.getId(),
                    PaymentType.DEPOSIT,
                    PaymentStatus.PAID_DEPOSIT
            );

            if (depositPaid) {
                appointment.setHoldExpiresAt(null);
                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                appointment.setPaymentStatus(PaymentStatus.PAID_DEPOSIT);
                appointmentRepository.save(appointment);
                continue;
            }

            paymentRepository.deleteByAppointment_Id(appointment.getId());
            appointment.getAddOns().clear();
            appointmentRepository.delete(appointment);
        }
    }
}
