package com.braided_beauty.braided_beauty.repositories;


import com.braided_beauty.braided_beauty.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    @Query(
            value = """
                SELECT * FROM appointments a
                JOIN services s ON a.service_id = s.id
                WHERE a.appointment_time < :desiredEnd
                AND (a.appointment_time + ((s.duration_minutes + :bufferMinutes) * interval '1 minute')) > :desiredStart
            """,
            nativeQuery = true
           )
    List<Appointment> findConflictingAppointments(@Param("desiredStart") LocalDateTime desiredStart,
                                                  @Param("desiredEnd") LocalDateTime desiredEnd,
                                                  @Param("bufferMinutes") int bufferMinutes
                                                  );

    List<Appointment> findAllByAppointmentTimeBetweenOrderByAppointmentTimeAsc(LocalDateTime start, LocalDateTime end);
    List<Appointment> findAllByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
}
