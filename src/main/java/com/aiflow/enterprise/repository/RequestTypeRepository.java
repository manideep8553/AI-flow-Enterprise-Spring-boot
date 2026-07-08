package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestTypeRepository extends MongoRepository<RequestType, String> {

    Optional<RequestType> findByName(String name);

    boolean existsByName(String name);

    Page<RequestType> findByActive(boolean active, Pageable pageable);

    Page<RequestType> findByCategory(String category, Pageable pageable);

    Page<RequestType> findByWorkflowId(String workflowId, Pageable pageable);

    List<RequestType> findByActiveTrue();
}
