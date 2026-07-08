package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends MongoRepository<Organization, String> {

    Optional<Organization> findByName(String name);

    Optional<Organization> findByRegistrationNumber(String registrationNumber);

    boolean existsByName(String name);

    boolean existsByRegistrationNumber(String registrationNumber);

    Page<Organization> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Organization> findByIndustry(String industry, Pageable pageable);

    Page<Organization> findByActive(boolean active, Pageable pageable);
}
