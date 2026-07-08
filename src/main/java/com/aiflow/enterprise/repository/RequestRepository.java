package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface RequestRepository extends MongoRepository<Request, String> {

    Page<Request> findByRequestTypeId(String requestTypeId, Pageable pageable);

    Page<Request> findBySubmittedBy(String submittedBy, Pageable pageable);

    Page<Request> findByStatus(RequestStatus status, Pageable pageable);

    Page<Request> findByCurrentApprover(String currentApprover, Pageable pageable);

    Page<Request> findByCurrentApproverAndStatus(String currentApprover, RequestStatus status, Pageable pageable);

    Page<Request> findByAssignedTo(String assignedTo, Pageable pageable);

    Page<Request> findByDepartmentId(String departmentId, Pageable pageable);

    Page<Request> findByRequestTypeIdAndStatus(String requestTypeId, RequestStatus status, Pageable pageable);

    Page<Request> findBySubmittedByAndStatus(String submittedBy, RequestStatus status, Pageable pageable);

    Page<Request> findBySubmittedByAndRequestTypeId(String submittedBy, String requestTypeId, Pageable pageable);

    Page<Request> findByTitleContainingIgnoreCase(String search, Pageable pageable);

    Page<Request> findByEscalatedTrue(Pageable pageable);

    Page<Request> findByDueDateBeforeAndStatusIn(Instant dueDate, List<RequestStatus> statuses, Pageable pageable);

    List<Request> findByStatusIn(List<RequestStatus> statuses);

    long countByRequestTypeId(String requestTypeId);

    long countByStatus(RequestStatus status);

    long countBySubmittedBy(String submittedBy);
}
