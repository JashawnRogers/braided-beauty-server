package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.enums.PaymentMethod;
import com.braided_beauty.braided_beauty.enums.PaymentStatus;
import com.braided_beauty.braided_beauty.enums.PaymentType;
import com.braided_beauty.braided_beauty.models.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByAppointment_IdAndPaymentType(UUID appointmentId, PaymentType paymentType);
    Optional<Payment> findByStripeSessionId(String sessionId);
    boolean existsByAppointment_IdAndPaymentTypeAndPaymentStatus(UUID appointmentId, PaymentType paymentType, PaymentStatus paymentStatus);
    void deleteByAppointment_Id(UUID appointmentId);

    @Query("""
    select coalesce(sum(p.amount + coalesce(p.tipAmount, 0)), 0)
    from Payment p
    join p.appointment a
    where a.completedAt between :start and :end
      and a.appointmentStatus = com.braided_beauty.braided_beauty.enums.AppointmentStatus.COMPLETED
      and p.paymentStatus in :statuses
      and p.paymentMethod = :method
    """)
    BigDecimal sumTotalForCompletedAppointmentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") java.util.Collection<PaymentStatus> statuses,
            @Param("method") PaymentMethod method
    );

    @Query("""
    select coalesce(sum(p.tipAmount), 0)
    from Payment p
    join p.appointment a
    where a.completedAt between :start and :end
      and a.appointmentStatus = com.braided_beauty.braided_beauty.enums.AppointmentStatus.COMPLETED
      and p.paymentStatus = :status
      and p.paymentMethod = :method
    """)
    BigDecimal sumTipForCompletedAppointmentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method
    );

    @Query("""
    select count(p)
    from Payment p
    join p.appointment a
    where a.completedAt between :start and :end
      and a.appointmentStatus = com.braided_beauty.braided_beauty.enums.AppointmentStatus.COMPLETED
      and p.paymentStatus = :status
      and p.paymentMethod = :method
      and p.paymentType = :type
    """)
    long countPaymentsForCompletedAppointmentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") PaymentStatus status,
            @Param("method") PaymentMethod method,
            @Param("type") PaymentType type
    );
}
