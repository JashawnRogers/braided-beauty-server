package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.dtos.refreshToken.IssuedRefreshTokenDTO;
import com.braided_beauty.braided_beauty.dtos.refreshToken.RotateTokensDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.LoginRequestDTO;
import com.braided_beauty.braided_beauty.dtos.user.auth.UserRegistrationDTO;
import com.braided_beauty.braided_beauty.dtos.user.global.ChangePasswordRequestDTO;
import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.exceptions.NotFoundException;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AccessTokenResponse;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.records.AuthResponse;
import com.braided_beauty.braided_beauty.records.OAuthIdentity;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import com.braided_beauty.braided_beauty.utils.PhoneNormalizer;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;


@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoyaltyService loyaltyService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public void login(LoginRequestDTO dto, HttpServletResponse res) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        IssuedRefreshTokenDTO issued = refreshTokenService.issueForUser(principal.id(),"web");
        refreshTokenService.addRefreshCookie(res, issued.getRefreshToken());
    }

    @Transactional
    public AccessTokenResponse refresh(String cookieToken, HttpServletResponse res) {
        if (cookieToken == null || cookieToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }

        RotateTokensDTO rotated = refreshTokenService.rotate(cookieToken);

        // write the *new* refresh token cookie (rotation)
        refreshTokenService.addRefreshCookie(res, rotated.getNewRefreshToken());

        return new AccessTokenResponse(rotated.getNewAccessToken());
    }

    public void logout(String cookieToken, HttpServletResponse res) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            refreshTokenService.revoke(cookieToken);
        }
        refreshTokenService.clearRefreshCookie(res);
    }

    @Transactional
    public AuthResponse registerAndIssueTokens(UserRegistrationDTO dto, HttpServletResponse res){
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalStateException("Email already in use.");
        }

        User newUser = new User();
        newUser.setEmail(dto.getEmail());
        newUser.setName(dto.getName() != null ? dto.getName() : dto.getEmail());
        newUser.setPhoneNumber(PhoneNormalizer.toE164(dto.getPhoneNumber()).orElse(null));
        newUser.setUserType(UserType.MEMBER);
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        User saved = userRepository.save(newUser);

        loyaltyService.attachLoyaltyRecord(saved.getId());
        loyaltyService.awardSignUpBonus(saved.getId());

        var authorities = UserType.roleStringsFor(saved.getUserType()).stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        String accessToken = jwtService.generateAccessToken(
                saved.getId(),
                saved.getEmail(),
                saved.getName(),
                authorities,
                saved.isEnabled()
        );


        var issued = refreshTokenService.issueForUser(saved.getId(), "web");
        refreshTokenService.addRefreshCookie(res, issued.getRefreshToken());
        return new AuthResponse(accessToken);
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
