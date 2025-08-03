package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.AddOn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, UUID> {
}
