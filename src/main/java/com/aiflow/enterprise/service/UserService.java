package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.UserRequest;
import com.aiflow.enterprise.dto.request.UserUpdateRequest;
import com.aiflow.enterprise.dto.response.UserResponse;
import org.springframework.data.domain.Page;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(String id);

    UserResponse getUserByUsername(String username);

    Page<UserResponse> getAllUsers(int page, int size, String role, String department, Boolean active);

    UserResponse updateUser(String id, UserUpdateRequest request);

    void deactivateUser(String id);

    void activateUser(String id);
}
