package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.Designation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignationRepository extends MongoRepository<Designation, String> {

    List<Designation> findByOrganizationId(String organizationId);

    Page<Designation> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<Designation> findByOrganizationIdAndTitle(String organizationId, String title);

    Page<Designation> findByOrganizationIdAndTitleContainingIgnoreCase(
            String organizationId, String title, Pageable pageable);
}
