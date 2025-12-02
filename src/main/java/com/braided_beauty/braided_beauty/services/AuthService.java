package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.DuplicateEntityException;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.LoyaltyRecord;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import com.braided_beauty.braided_beauty.utils.PhoneNormalizer;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(UserRegistrationDTO dto){
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateEntityException("Email already in use.");
        }

        User newUser = new User();
        newUser.setEmail(dto.getEmail());
        newUser.setName(dto.getName() != null ? dto.getName() : dto.getEmail());
        newUser.setPhoneNumber(PhoneNormalizer.toE164(dto.getPhoneNumber()).orElse(null));
        newUser.setUserType(UserType.MEMBER);
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        LoyaltyRecord loyaltyRecord = new LoyaltyRecord(newUser);
        newUser.setLoyaltyRecord(loyaltyRecord);

        return userRepository.save(newUser);
    }

    @Transactional
    public void changePassword(UUID id, String newPassword){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with provided ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public Authentication toAppAuthentication(Authentication oauthAuth){
        // Extract identity from provider principal
        Map<String, Object> attributes = extractAttributes(oauthAuth);
        String email = (String) attributes.getOrDefault("email", oauthAuth.getName());
        String name = (String) attributes.getOrDefault("name", email);

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("OAuth provider did not supply an email address.");
        }

        // Find user or create new if not in db
        User user = userRepository.findUserByEmail(email)
                .orElseGet(() -> userService.registerFromOauth(attributes));

        // Build authorities and principal
        Set<String> roleStrings = UserType.roleStringsFor(user.getUserType());
        var authorities = roleStrings.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        var principal = new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getName() != null ? user.getName() : name,
                authorities
        );

        // Return authenticated token
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }


    private Map<String, Object> extractAttributes(Authentication oauthAuth) {
        Object principalObject = oauthAuth.getPrincipal();

        if (principalObject instanceof OidcUser oidcUser){
            Map<String, Object> m = new HashMap<>(oidcUser.getClaims());
            m.putIfAbsent("email", oidcUser.getEmail());
            m.putIfAbsent("name", oidcUser.getName());
            return m;
        }
        if (principalObject instanceof OAuth2User oAuth2User){
            return new HashMap<>(oAuth2User.getAttributes());
        }

        return Map.of("email", oauthAuth.getName());
    }
}
