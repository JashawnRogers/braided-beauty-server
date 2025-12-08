package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.ChangePasswordRequestDTO;
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

    // Creates account in local db for first time OAuth login
    public User registerFromOauth(Map<String, Object> attributes){
        String email = (String) attributes.get("email");
        String name = (String) attributes.getOrDefault("name", email);
        String providerId = (String) attributes.getOrDefault("sub", null);

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setUserType(UserType.MEMBER);

        LoyaltyRecord loyaltyRecord = new LoyaltyRecord();
        loyaltyRecord.setEnabled(true);
        loyaltyRecord.setPoints(0);
        loyaltyRecord.setRedeemedPoints(0);
        loyaltyRecord.setSignupBonusAwarded(false);

        loyaltyRecord.setUser(user);
        user.setLoyaltyRecord(loyaltyRecord);

        return userRepository.save(user);
    }

    @Transactional
    public void updatePassword(UUID id, ChangePasswordRequestDTO dto){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with provided ID: " + id));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New password and confirmation do not match");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        validatePassword(dto.getNewPassword(), user);

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
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
                .orElseGet(() -> registerFromOauth(attributes));

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

    private void validatePassword(String password, User user) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be equal to or more than 8 characters");
        }

        String lower = password.toLowerCase();

        if (user.getName() != null && lower.contains(user.getName().toLowerCase())) {
            throw new IllegalArgumentException("Password must not include your name");
        }

        if (user.getEmail() != null && lower.contains(user.getEmail().toLowerCase())) {
            throw new IllegalArgumentException("Password must not include email");
        }

        if (user.getPhoneNumber() != null && password.contains(user.getPhoneNumber())) {
            throw new IllegalArgumentException("Password must not include phone number");
        }
    }
}
