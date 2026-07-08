package com.aiflow.enterprise.org.repository;

import com.aiflow.enterprise.entity.WorkflowTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTemplateRepository extends MongoRepository<WorkflowTemplate, String> {

    Optional<WorkflowTemplate> findByName(String name);

    Page<WorkflowTemplate> findByPublished(boolean published, Pageable pageable);

    Page<WorkflowTemplate> findByCategory(String category, Pageable pageable);

    Page<WorkflowTemplate> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<WorkflowTemplate> findByPublishedOrderByUsageCountDesc(boolean published);
}
