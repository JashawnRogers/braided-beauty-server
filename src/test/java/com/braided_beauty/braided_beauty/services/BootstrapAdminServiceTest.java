package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.BadRequestException;
import com.braided_beauty.braided_beauty.exceptions.ConflictException;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.BootstrapAdminProperties;
import com.braided_beauty.braided_beauty.records.BootstrapAdminResponse;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    private BootstrapAdminProperties properties;

    private BootstrapAdminService bootstrapAdminService;

    @BeforeEach
    void setUp() {
        properties = new BootstrapAdminProperties(true, "top-secret");
        bootstrapAdminService = new BootstrapAdminService(userRepository, properties);
    }

    @Test
    void bootstrapCurrentUser_rejectsWhenDisabled() {
        bootstrapAdminService = new BootstrapAdminService(userRepository, new BootstrapAdminProperties(false, "top-secret"));

        assertThrows(BadRequestException.class,
                () -> bootstrapAdminService.bootstrapCurrentUser(UUID.randomUUID(), "top-secret"));

        verify(userRepository, never()).existsByUserType(any());
    }

    @Test
    void bootstrapCurrentUser_rejectsWhenAdminAlreadyExists() {
        when(userRepository.existsByUserType(UserType.ADMIN)).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> bootstrapAdminService.bootstrapCurrentUser(UUID.randomUUID(), "top-secret"));

        verify(userRepository, never()).findById(any());
    }

    @Test
    void bootstrapCurrentUser_rejectsWhenSecretDoesNotMatch() {
        when(userRepository.existsByUserType(UserType.ADMIN)).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> bootstrapAdminService.bootstrapCurrentUser(UUID.randomUUID(), "wrong-secret"));

        verify(userRepository, never()).findById(any());
    }

    @Test
    void bootstrapCurrentUser_promotesAuthenticatedUserToAdmin() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUserType(UserType.MEMBER);

        when(userRepository.existsByUserType(UserType.ADMIN)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        BootstrapAdminResponse response = bootstrapAdminService.bootstrapCurrentUser(userId, "top-secret");

        assertEquals("Authenticated user promoted to ADMIN.", response.message());
        assertEquals(UserType.ADMIN, user.getUserType());
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }
}
