package com.aiflow.enterprise.security;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.enums.UserRole;
import com.aiflow.enterprise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class OAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(OAuth2UserService.class);

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);
        String name = extractName(registrationId, attributes);
        String avatarUrl = extractAvatar(registrationId, attributes);
        String providerId = extractProviderId(registrationId, attributes);

        User user = processOAuthUser(email, name, avatarUrl, registrationId, providerId);

        CustomUserDetails customUserDetails = new CustomUserDetails(user);
        customUserDetails.setAttributes(attributes);
        return customUserDetails;
    }

    private User processOAuthUser(String email, String name, String avatarUrl,
                                  String provider, String providerId) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setLastLoginAt(java.time.Instant.now());
            user.setAvatarUrl(avatarUrl);
            user.setAuthProvider(provider);
            user.setAuthProviderId(providerId);
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                user.setEmailVerifiedAt(java.time.Instant.now());
            }
            return userRepository.save(user);
        }

        String username = generateUniqueUsername(email);

        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(UUID.randomUUID().toString())
                .firstName(name.contains(" ") ? name.substring(0, name.lastIndexOf(' ')) : name)
                .lastName(name.contains(" ") ? name.substring(name.lastIndexOf(' ') + 1) : "")
                .role(UserRole.OPERATOR)
                .active(true)
                .emailVerified(true)
                .emailVerifiedAt(java.time.Instant.now())
                .authProvider(provider)
                .authProviderId(providerId)
                .avatarUrl(avatarUrl)
                .build();

        return userRepository.save(newUser);
    }

    private String extractEmail(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("email");
        } else if ("microsoft".equals(registrationId)) {
            return (String) attributes.get("mail") != null
                    ? (String) attributes.get("mail")
                    : (String) attributes.get("userPrincipalName");
        }
        return (String) attributes.get("email");
    }

    private String extractName(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("name");
        } else if ("microsoft".equals(registrationId)) {
            return (String) attributes.get("displayName");
        }
        return (String) attributes.get("name");
    }

    private String extractAvatar(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("picture");
        }
        return null;
    }

    private String extractProviderId(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return (String) attributes.get("sub");
        } else if ("microsoft".equals(registrationId)) {
            return (String) attributes.get("id");
        }
        return (String) attributes.get("sub");
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase();
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + suffix++;
        }
        return username;
    }
}
