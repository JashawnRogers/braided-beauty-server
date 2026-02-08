package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.dtos.service.ServiceResponseDTO;
import com.braided_beauty.braided_beauty.enums.AppointmentStatus;
import com.braided_beauty.braided_beauty.models.ServiceModel;
import com.braided_beauty.braided_beauty.records.ServicePopularityRow;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<ServiceModel, UUID>, JpaSpecificationExecutor<ServiceModel> {
    boolean existsByName(String name);

    Optional<ServiceModel> findTopByOrderByTimesBookedDesc();

    boolean existsByCategoryId(UUID id);

    @EntityGraph(attributePaths = {"category", "addOns"})
    @Nonnull
    Page<ServiceModel> findAll(@Nullable Specification<ServiceModel> spec, @Nonnull Pageable pageable);

    Optional<List<ServiceModel>> findAllByCategoryId(UUID categoryId);


    // ---------- Monthly: most popular ----------
    @Query("""
        select new com.braided_beauty.braided_beauty.records.ServicePopularityRow(
            s.id,
            s.name,
            count(a.id)
        )
        from ServiceModel s
        join s.appointments a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.completedAt between :start and :end
        group by s.id, s.name
        order by count(a.id) desc
    """)
    List<ServicePopularityRow> findMostPopularServicesByCompletedBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // ---------- Monthly: least popular (among those with >=1 completion) ----------
    @Query("""
        select new com.braided_beauty.braided_beauty.records.ServicePopularityRow(
            s.id,
            s.name,
            count(a.id)
        )
        from ServiceModel s
        join s.appointments a
        where a.appointmentStatus = :status
          and a.completedAt is not null
          and a.completedAt between :start and :end
        group by s.id, s.name
        having count(a.id) > 0
        order by count(a.id) asc
    """)
    List<ServicePopularityRow> findLeastPopularServicesByCompletedBetween(
            @Param("status") AppointmentStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    // ---------- All-time: most popular ----------
    @Query("""
        select new com.braided_beauty.braided_beauty.records.ServicePopularityRow(
            s.id,
            s.name,
            count(a.id)
        )
        from ServiceModel s
        join s.appointments a
        where a.appointmentStatus = :status
          and a.completedAt is not null
        group by s.id, s.name
        order by count(a.id) desc
    """)
    List<ServicePopularityRow> findMostPopularServicesAllTime(
            @Param("status") AppointmentStatus status,
            Pageable pageable
    );

    // ---------- All-time: least popular (among those with >=1 completion) ----------
    @Query("""
        select new com.braided_beauty.braided_beauty.records.ServicePopularityRow(
            s.id,
            s.name,
            count(a.id)
        )
        from ServiceModel s
        join s.appointments a
        where a.appointmentStatus = :status
          and a.completedAt is not null
        group by s.id, s.name
        having count(a.id) > 0
        order by count(a.id) asc
    """)
    List<ServicePopularityRow> findLeastPopularServicesAllTime(
            @Param("status") AppointmentStatus status,
            Pageable pageable
    );
}
