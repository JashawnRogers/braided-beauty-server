package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
