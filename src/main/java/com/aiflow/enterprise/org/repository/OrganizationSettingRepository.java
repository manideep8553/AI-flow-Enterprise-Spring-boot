package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.OrganizationSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationSettingRepository extends MongoRepository<OrganizationSetting, String> {

    Optional<OrganizationSetting> findByOrganizationId(String organizationId);

    void deleteByOrganizationId(String organizationId);
}
