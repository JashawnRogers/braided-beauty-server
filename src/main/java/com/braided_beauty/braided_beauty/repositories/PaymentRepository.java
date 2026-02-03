package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByAppointment_IdAndPaymentType(UUID appointmentId, PaymentType paymentType);
    Optional<Payment> findByStripeSessionId(String sessionId);
    boolean existsByAppointment_IdAndPaymentTypeAndPaymentStatus(UUID appointmentId, PaymentType paymentType, PaymentStatus paymentStatus);
    void deleteByAppointment_Id(UUID appointmentId);

}
