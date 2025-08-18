package com.braided_beauty.braided_beauty.records;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AppUserPrincipal implements OAuth2User, UserDetails {
    private final UUID id;
    private final String email;
    private final String userType;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // Constructor for OAuth since it does not require a password
    public AppUserPrincipal (
            UUID id,
            String email,
            String userType,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.userType = userType;
        this.password = null;
        this.authorities = authorities;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return id.toString(); // Spring uses this as a unique identifier, id is more specific and unique than a user's name
    }
}
