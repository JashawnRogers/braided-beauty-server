package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<UUID, PromoCode> {
    Optional<PromoCode> findByCodeIgnoreCase(String discountCode);

    boolean existsByCodeIgnoreCase(String discountCode);
}
