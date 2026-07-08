package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {

    List<Team> findByDepartmentId(String departmentId);

    Page<Team> findByDepartmentId(String departmentId, Pageable pageable);

    Page<Team> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<Team> findByDepartmentIdAndName(String departmentId, String name);

    Page<Team> findByDepartmentIdAndNameContainingIgnoreCase(
            String departmentId, String name, Pageable pageable);

    long countByDepartmentId(String departmentId);
}
