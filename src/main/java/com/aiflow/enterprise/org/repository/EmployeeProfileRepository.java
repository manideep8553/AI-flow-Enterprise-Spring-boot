package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.EmployeeProfile;
import com.aiflow.enterprise.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeProfileRepository extends MongoRepository<EmployeeProfile, String> {

    Optional<EmployeeProfile> findByUserId(String userId);

    Optional<EmployeeProfile> findByOrganizationIdAndEmployeeId(String organizationId, String employeeId);

    Page<EmployeeProfile> findByOrganizationId(String organizationId, Pageable pageable);

    Page<EmployeeProfile> findByDepartmentId(String departmentId, Pageable pageable);

    Page<EmployeeProfile> findByTeamIdsContaining(String teamId, Pageable pageable);

    Page<EmployeeProfile> findByDesignationId(String designationId, Pageable pageable);

    Page<EmployeeProfile> findByReportingManagerId(String managerId, Pageable pageable);

    Page<EmployeeProfile> findByStatus(EmployeeStatus status, Pageable pageable);

    Page<EmployeeProfile> findByOrganizationIdAndStatus(String organizationId, EmployeeStatus status, Pageable pageable);

    List<EmployeeProfile> findByReportingManagerId(String managerId);

    List<EmployeeProfile> findByOrganizationId(String organizationId);

    long countByOrganizationId(String organizationId);

    long countByDepartmentId(String departmentId);

    long countByStatus(EmployeeStatus status);
}
