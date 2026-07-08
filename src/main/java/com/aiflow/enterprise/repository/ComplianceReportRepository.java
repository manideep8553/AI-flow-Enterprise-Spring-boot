package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.ComplianceReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ComplianceReportRepository extends MongoRepository<ComplianceReport, String> {

    Page<ComplianceReport> findByReportType(String reportType, Pageable pageable);

    Page<ComplianceReport> findByGeneratedBy(String generatedBy, Pageable pageable);

    Page<ComplianceReport> findByStatus(String status, Pageable pageable);

    List<ComplianceReport> findByGeneratedAtBetween(Instant from, Instant to);

    Page<ComplianceReport> findByReportTypeAndGeneratedAtBetween(
            String reportType, Instant from, Instant to, Pageable pageable);

    long countByReportType(String reportType);

    long countByStatus(String status);
}
