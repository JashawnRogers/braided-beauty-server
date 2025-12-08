package com.braided_beauty.braided_beauty.dtos.user.global;

import com.braided_beauty.braided_beauty.enums.LoyaltyTier;
import com.braided_beauty.braided_beauty.enums.UserType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class CurrentUserDTO {
    private UUID id;
    private String name;
    private String email;
    private String phoneNumber;
    private UserType memberStatus;
    private Integer loyaltyPoints;
    private Integer redeemedPoints;
    private LoyaltyTier loyaltyTier;
    private String oAuthSubject;
    private String oAuthProvider;
    private Boolean isOAuthAccount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
