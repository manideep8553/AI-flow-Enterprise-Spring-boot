package com.aiflow.enterprise.security;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean active;
    private final boolean emailVerified;
    private final boolean accountLocked;
    private final UserRole role;
    private final Set<GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.active = user.getActive() != null && user.getActive();
        this.emailVerified = user.isEmailVerified();
        this.accountLocked = user.isAccountLocked();
        this.role = user.getRole();
        this.authorities = user.getAuthorities() != null
                ? user.getAuthorities().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet())
                : Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
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
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && emailVerified;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}
