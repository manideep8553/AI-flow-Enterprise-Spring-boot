package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.UserRequest;
import com.aiflow.enterprise.dto.request.UserUpdateRequest;
import com.aiflow.enterprise.dto.response.UserResponse;
import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.enums.UserRole;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.UserMapper;
import com.aiflow.enterprise.repository.UserRepository;
import com.aiflow.enterprise.service.UserService;
import com.aiflow.enterprise.util.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }
        User user = userMapper.toEntity(request);
        user.setPasswordHash(EncryptionUtils.hashPassword(request.getPassword()));
        User saved = userRepository.save(user);
        log.info("User created: {} with username {}", saved.getId(), saved.getUsername());
        return userMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(int page, int size, String role, String department, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;

        if (role != null) {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            userPage = userRepository.findByRole(userRole, pageable);
        } else if (department != null) {
            userPage = userRepository.findByDepartment(department, pageable);
        } else if (active != null) {
            userPage = userRepository.findByActive(active, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(userMapper::toResponse);
    }

    @Override
    public UserResponse updateUser(String id, UserUpdateRequest request) {
        User existing = findUserOrThrow(id);
        userMapper.updateEntity(request, existing);
        User saved = userRepository.save(existing);
        log.info("User updated: {}", id);
        return userMapper.toResponse(saved);
    }

    @Override
    public void deactivateUser(String id) {
        User user = findUserOrThrow(id);
        if (!user.getActive()) {
            throw new com.aiflow.enterprise.exception.BadRequestException("User is already deactivated");
        }
        user.setActive(false);
        userRepository.save(user);
        log.info("User deactivated: {}", id);
    }

    @Override
    public void activateUser(String id) {
        User user = findUserOrThrow(id);
        if (user.getActive()) {
            throw new com.aiflow.enterprise.exception.BadRequestException("User is already active");
        }
        user.setActive(true);
        userRepository.save(user);
        log.info("User activated: {}", id);
    }

    private User findUserOrThrow(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
