package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.Fee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface FeeRepository extends JpaRepository<Fee, UUID>, JpaSpecificationExecutor<Fee> {
    boolean existsByNameIgnoreCase(String name);
}
