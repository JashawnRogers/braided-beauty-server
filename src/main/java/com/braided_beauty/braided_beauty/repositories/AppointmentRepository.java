package com.braided_beauty.braided_beauty.repositories;


import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query(
            value = """
                SELECT a.*
                FROM appointments a
                WHERE a.appointment_time < :desiredEnd
                AND (a.appointment_time + ((a.duration_minutes + :bufferMinutes) * interval '1 minute')) > :desiredStart
                
                AND (
                    a.appointment_status = 'CONFIRMED'
                    
                    OR (
                    a.appointment_status = 'PENDING_CONFIRMATION'
                    AND a.hold_expires_at IS NOT NULL
                    AND a.hold_expires_at > now())
                )
            """, nativeQuery = true
           )
    List<Appointment> findConflictingAppointments(@Param("desiredStart") LocalDateTime desiredStart,
                                                  @Param("desiredEnd") LocalDateTime desiredEnd,
                                                  @Param("bufferMinutes") int bufferMinutes
                                                  );

    @Query(
            value = """
        SELECT a.*
        FROM appointments a
        WHERE a.appointment_time >= :start
          AND a.appointment_time < :end
          AND (
                a.appointment_status IN ('CONFIRMED', 'COMPLETED', 'NO_SHOW')
                OR (
                    a.appointment_status = 'PENDING_CONFIRMATION'
                    AND a.hold_expires_at IS NOT NULL
                    AND a.hold_expires_at > now()
                )
          )
        ORDER BY a.appointment_time ASC
    """,
            nativeQuery = true
    )
    List<Appointment> findBlockingAppointmentsForWindow(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query(value = """
    SELECT a.*
    FROM appointments a
    WHERE a.appointment_status = 'PENDING_CONFIRMATION'
      AND a.hold_expires_at IS NOT NULL
      AND a.hold_expires_at <= now()
""", nativeQuery = true)
    List<Appointment> findExpiredPendingHolds();

    Optional<Appointment> findByStripeSessionId(String sessionId);
    List<Appointment> findAllByAppointmentTimeBetweenOrderByAppointmentTimeAsc(LocalDateTime start, LocalDateTime end);
    Optional<Appointment> findFirstByUserIdAndAppointmentTimeAfterAndAppointmentStatusInOrderByAppointmentTimeAsc(UUID userId, LocalDateTime now, Collection<AppointmentStatus> status);
    boolean existsByPromoCode_IdAndAppointmentStatusIn(UUID promoCodeId, Collection<AppointmentStatus> statuses);

    Page<Appointment> findByUser_IdAndAppointmentStatusInOrderByAppointmentTimeDesc(
            UUID userId,
            Collection<AppointmentStatus> statuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id IS NOT NULL")
    List<Appointment> lockSchedule();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.guestCancelToken = :token")
    Optional<Appointment> findByGuestCancelTokenForUpdate(String token);

    @Query("SELECT a FROM Appointment a WHERE a.guestCancelToken = :token")
    Optional<Appointment> findByGuestCancelToken(String token);

    long countByAppointmentStatusAndCompletedAtBetween(
            AppointmentStatus status,
            LocalDateTime start,
            LocalDateTime end
    );

    long countByAppointmentStatusAndCompletedAtIsNotNull(AppointmentStatus status);

    @Query("""
        select coalesce(sum(a.totalAmount), 0)
        from Appointment a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.completedAt between :start and :end
          and a.stripePaymentId is not null
    """)
    BigDecimal sumCardTotalByCompletedBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select coalesce(sum(a.totalAmount), 0)
        from Appointment a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.completedAt between :start and :end
          and a.stripePaymentId is null
    """)
    BigDecimal sumCashTotalByCompletedBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        select coalesce(sum(a.totalAmount), 0)
        from Appointment a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.stripePaymentId is not null
    """)
    BigDecimal sumCardTotalAllTime(@Param("status") AppointmentStatus status);

    @Query("""
        select coalesce(sum(a.totalAmount), 0)
        from Appointment a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.stripePaymentId is null
    """)
    BigDecimal sumCashTotalAllTime(@Param("status") AppointmentStatus status);
}
