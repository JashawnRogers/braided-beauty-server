package com.braided_beauty.braided_beauty.repositories;


import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.models.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    List<Appointment> findAllByAppointmentTimeBetweenOrderByAppointmentTimeAsc(LocalDateTime start, LocalDateTime end);
    List<Appointment> findAllByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
    Optional<Appointment> findFirstByUserIdAndAppointmentTimeAfterAndAppointmentStatusInOrderByAppointmentTimeAsc(UUID userId, LocalDateTime now,
                                                                                       Collection<AppointmentStatus> status);
    Page<Appointment> findUserByIdAndAppointmentTimeBeforeAndAppointmentStatusIn(
            UUID userId,
            LocalDateTime now,
            Collection<AppointmentStatus> statuses,
            Pageable pageable
    );
    List<Appointment> findByServiceIdAndAppointmentTimeBetween(UUID serviceId, LocalDateTime startTime, LocalDateTime closeTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id IS NOT NULL")
    List<Appointment> lockSchedule();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.guestCancelToken = :token")
    Optional<Appointment> findByGuestCancelTokenForUpdate(String token);
}
