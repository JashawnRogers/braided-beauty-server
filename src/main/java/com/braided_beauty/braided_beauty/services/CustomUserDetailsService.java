package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        var authorities = switch(user.getUserType()) {
            case ADMIN -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case MEMBER -> List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
            case GUEST -> List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        };

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(false)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();

    }
}
