package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Designation;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.DesignationRequest;
import com.aiflow.enterprise.org.dto.response.DesignationResponse;
import com.aiflow.enterprise.org.repository.DesignationRepository;
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
public class DesignationService {

    private static final Logger log = LoggerFactory.getLogger(DesignationService.class);
    private final DesignationRepository repository;

    public DesignationService(DesignationRepository repository) { this.repository = repository; }

    public DesignationResponse create(DesignationRequest req) {
        if (repository.findByOrganizationIdAndTitle(req.getOrganizationId(), req.getTitle()).isPresent())
            throw new DuplicateResourceException("Designation", "title", req.getTitle());
        Designation d = Designation.builder().organizationId(req.getOrganizationId())
                .title(req.getTitle()).description(req.getDescription()).level(req.getLevel())
                .grade(req.getGrade()).skills(req.getSkills()).careerPath(req.getCareerPath())
                .active(true).build();
        return toResponse(repository.save(d));
    }

    public DesignationResponse update(String id, DesignationRequest req) {
        Designation d = findDes(id);
        d.setTitle(req.getTitle()); d.setDescription(req.getDescription()); d.setLevel(req.getLevel());
        d.setGrade(req.getGrade()); d.setSkills(req.getSkills()); d.setCareerPath(req.getCareerPath());
        return toResponse(repository.save(d));
    }

    @Transactional(readOnly = true)
    public DesignationResponse getById(String id) { return toResponse(findDes(id)); }

    @Transactional(readOnly = true)
    public Page<DesignationResponse> getByOrganization(String orgId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "level"));
        Page<Designation> desPage = search != null
                ? repository.findByOrganizationIdAndTitleContainingIgnoreCase(orgId, search, pageable)
                : repository.findByOrganizationId(orgId, pageable);
        return desPage.map(this::toResponse);
    }

    public void delete(String id) { repository.delete(findDes(id)); }

    private Designation findDes(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Designation", "id", id));
    }

    private DesignationResponse toResponse(Designation d) {
        return DesignationResponse.builder().id(d.getId()).organizationId(d.getOrganizationId())
                .title(d.getTitle()).description(d.getDescription()).level(d.getLevel())
                .grade(d.getGrade()).skills(d.getSkills()).careerPath(d.getCareerPath())
                .active(d.isActive()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
