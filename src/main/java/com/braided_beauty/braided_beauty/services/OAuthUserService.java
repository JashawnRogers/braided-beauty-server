package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.OAuthIdentity;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class OAuthUserService {
    private final UserRepository userRepository;
    private final LoyaltyService loyaltyService;

    @Transactional
    public User upsertFromOauth(OAuthIdentity id) {
        return userRepository.findByOauthProviderAndOauthSubject(id.provider(), id.subject())
                .orElseGet(() -> {
                    // if email exists, link it
                    var byEmail = userRepository.findUserByEmail(id.email());
                    if (byEmail.isPresent()) {
                        User existing = byEmail.get();
                        existing.setOauthProvider(id.provider());
                        existing.setOauthSubject(id.subject());
                        return userRepository.save(existing);
                    }

                    User u = new User();
                    u.setEmail(id.email());
                    u.setName((id.name() != null && !id.name().isBlank()) ? id.name() : id.email());
                    u.setUserType(UserType.MEMBER);
                    u.setOauthProvider(id.provider());
                    u.setOauthSubject(id.subject());

                    User saved = userRepository.save(u);
                    loyaltyService.attachLoyaltyRecord(saved.getId());
                    loyaltyService.awardSignUpBonus(saved.getId());
                    return saved;
                });
    }

    public OAuthIdentity extractIdentity(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        String provider = (authentication instanceof OAuth2AuthenticationToken token)
                ? token.getAuthorizedClientRegistrationId()
                : "Unknown";

        if (principal instanceof OidcUser oidcUser) {
            String subject = oidcUser.getSubject();
            String email = oidcUser.getEmail();
            String name = oidcUser.getFullName();

            return new OAuthIdentity(provider, subject, name, email);
        }

        if (principal instanceof OAuth2User oAuth2User) {
            String subject = oAuth2User.getAttribute("sub");
            String email = oAuth2User.getAttribute("email");
            String name = oAuth2User.getAttribute("name");

            return new OAuthIdentity(provider, subject, name, email);
        }

        throw new IllegalArgumentException("Unsupported OAuth principal type: " + authentication.getPrincipal());
    }
}
