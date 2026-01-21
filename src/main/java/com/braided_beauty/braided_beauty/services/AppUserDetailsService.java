package com.braided_beauty.braided_beauty.services;

import com.braided_beauty.braided_beauty.enums.UserType;
import com.braided_beauty.braided_beauty.models.User;
import com.braided_beauty.braided_beauty.records.AppUserPrincipal;
import com.braided_beauty.braided_beauty.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User u = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        var authorities = UserType.roleStringsFor(u.getUserType()).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();

        // IMPORTANT: AppUserPrincipal must implement UserDetails
        return new AppUserPrincipal(
                u.getId(),
                u.getEmail(),
                u.getName() != null ? u.getName() : u.getEmail(),
                authorities,
                u.getPassword(),      // include for password auth
                u.isEnabled()
        );
    }
}
