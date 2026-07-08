package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByDepartment(String department, Pageable pageable);

    Page<User> findByActive(Boolean active, Pageable pageable);

    long countByEmailVerified(boolean emailVerified);
}
