package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Trigger;
import com.aiflow.enterprise.enums.TriggerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TriggerRepository extends MongoRepository<Trigger, String> {

    Page<Trigger> findByWorkflowId(String workflowId, Pageable pageable);

    Page<Trigger> findByType(TriggerType type, Pageable pageable);

    Page<Trigger> findByActive(Boolean active, Pageable pageable);

    List<Trigger> findByActiveAndType(Boolean active, TriggerType type);
}
