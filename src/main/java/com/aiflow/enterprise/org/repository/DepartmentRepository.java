package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends MongoRepository<Department, String> {

    List<Department> findByOrganizationId(String organizationId);

    Page<Department> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<Department> findByOrganizationIdAndName(String organizationId, String name);

    Page<Department> findByOrganizationIdAndNameContainingIgnoreCase(
            String organizationId, String name, Pageable pageable);

    Page<Department> findByOrganizationIdAndActive(String organizationId, boolean active, Pageable pageable);

    long countByOrganizationId(String organizationId);
}
