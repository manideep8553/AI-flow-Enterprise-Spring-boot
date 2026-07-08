package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.BusinessUnit;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.BusinessUnitRequest;
import com.aiflow.enterprise.org.dto.response.BusinessUnitResponse;
import com.aiflow.enterprise.org.repository.BusinessUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BusinessUnitService {

    private static final Logger log = LoggerFactory.getLogger(BusinessUnitService.class);
    private final BusinessUnitRepository repository;

    public BusinessUnitService(BusinessUnitRepository repository) { this.repository = repository; }

    public BusinessUnitResponse create(BusinessUnitRequest req) {
        if (repository.findByOrganizationIdAndName(req.getOrganizationId(), req.getName()).isPresent())
            throw new DuplicateResourceException("BusinessUnit", "name", req.getName());
        BusinessUnit bu = BusinessUnit.builder().organizationId(req.getOrganizationId())
                .name(req.getName()).code(req.getCode()).description(req.getDescription())
                .headEmployeeId(req.getHeadEmployeeId()).budgetCode(req.getBudgetCode())
                .metadata(req.getMetadata()).active(true).build();
        return toResponse(repository.save(bu));
    }

    public BusinessUnitResponse update(String id, BusinessUnitRequest req) {
        BusinessUnit bu = findBu(id);
        bu.setName(req.getName()); bu.setCode(req.getCode());
        bu.setDescription(req.getDescription()); bu.setHeadEmployeeId(req.getHeadEmployeeId());
        bu.setBudgetCode(req.getBudgetCode()); bu.setMetadata(req.getMetadata());
        return toResponse(repository.save(bu));
    }

    @Transactional(readOnly = true)
    public BusinessUnitResponse getById(String id) { return toResponse(findBu(id)); }

    @Transactional(readOnly = true)
    public Page<BusinessUnitResponse> getByOrganization(String orgId, int page, int size, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<BusinessUnit> buPage = active != null
                ? repository.findByOrganizationIdAndActive(orgId, active, pageable)
                : repository.findByOrganizationId(orgId, pageable);
        return buPage.map(this::toResponse);
    }

    public void delete(String id) { repository.delete(findBu(id)); }

    private BusinessUnit findBu(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessUnit", "id", id));
    }

    private BusinessUnitResponse toResponse(BusinessUnit b) {
        return BusinessUnitResponse.builder().id(b.getId()).organizationId(b.getOrganizationId())
                .name(b.getName()).code(b.getCode()).description(b.getDescription())
                .headEmployeeId(b.getHeadEmployeeId()).budgetCode(b.getBudgetCode())
                .active(b.isActive()).metadata(b.getMetadata())
                .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt()).build();
    }
}
