package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.BusinessUnit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessUnitRepository extends MongoRepository<BusinessUnit, String> {

    List<BusinessUnit> findByOrganizationId(String organizationId);

    Page<BusinessUnit> findByOrganizationId(String organizationId, Pageable pageable);

    Optional<BusinessUnit> findByOrganizationIdAndName(String organizationId, String name);

    Page<BusinessUnit> findByOrganizationIdAndActive(String organizationId, boolean active, Pageable pageable);
}
