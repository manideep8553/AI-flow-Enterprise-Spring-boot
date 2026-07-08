package com.aiflow.enterprise.security;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.repository.UserRepository;
import jakarta.validation.constraints.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with identifier: {}", usernameOrEmail);
                    return new UsernameNotFoundException(
                            "User not found with username or email: " + usernameOrEmail);
                });
        return new CustomUserDetails(user);
    }

    public CustomUserDetails loadUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        return new CustomUserDetails(user);
    }

    private Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        if (usernameOrEmail != null && usernameOrEmail.contains("@")) {
            return userRepository.findByEmail(usernameOrEmail);
        }
        return userRepository.findByUsername(usernameOrEmail);
    }
}
