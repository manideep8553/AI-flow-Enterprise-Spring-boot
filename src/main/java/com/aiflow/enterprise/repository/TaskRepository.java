package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Task;
import com.aiflow.enterprise.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {

    Page<Task> findByWorkflowId(String workflowId, Pageable pageable);

    Page<Task> findByExecutionId(String executionId, Pageable pageable);

    Page<Task> findByAssignee(String assignee, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByWorkflowIdAndStatus(String workflowId, TaskStatus status, Pageable pageable);

    List<Task> findByExecutionId(String executionId);
}
