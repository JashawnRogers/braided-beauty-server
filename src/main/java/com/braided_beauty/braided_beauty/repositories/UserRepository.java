package com.braided_beauty.braided_beauty.repositories;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    Optional<User> findUserByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUserType(UserType userType);
    boolean existsByOauthProviderAndOauthSubject(String oAuthProvider, String oAuthSubject);
    Optional<User> findByOauthProviderAndOauthSubject(String oAuthProvider, String oAuthSubject);
    Optional<User> findByEmail(String normalizedEmail);
}
