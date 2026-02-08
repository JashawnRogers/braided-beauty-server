package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    List<User> findAllByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
    List<User> findAllByUserType(UserType userType);
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByOauthProviderAndOauthSubject(String oAuthProvider, String oAuthSubject);
    Optional<User> findByOauthProviderAndOauthSubject(String oAuthProvider, String oAuthSubject);
    long countByUserType(UserType userType);
}
