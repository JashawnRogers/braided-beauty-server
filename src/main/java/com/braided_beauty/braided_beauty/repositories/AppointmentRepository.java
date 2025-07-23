package com.braided_beauty.braided_beauty.repositories;


import com.braided_beauty.braided_beauty.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
}
