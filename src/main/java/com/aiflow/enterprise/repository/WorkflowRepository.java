package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.enums.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends MongoRepository<Workflow, String> {

    Optional<Workflow> findByName(String name);

    boolean existsByName(String name);

    Page<Workflow> findByStatus(WorkflowStatus status, Pageable pageable);

    Page<Workflow> findByTagsContaining(String tag, Pageable pageable);

    Page<Workflow> findByCategory(String category, Pageable pageable);

    Page<Workflow> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Workflow> findByStatusOrderByUpdatedAtDesc(WorkflowStatus status);
}
