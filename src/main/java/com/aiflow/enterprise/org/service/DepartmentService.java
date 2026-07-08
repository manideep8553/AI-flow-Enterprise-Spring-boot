package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Department;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.DepartmentRequest;
import com.aiflow.enterprise.org.dto.response.DepartmentResponse;
import com.aiflow.enterprise.org.repository.DepartmentRepository;
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
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public DepartmentResponse create(DepartmentRequest req) {
        if (departmentRepository.findByOrganizationIdAndName(req.getOrganizationId(), req.getName()).isPresent()) {
            throw new DuplicateResourceException("Department", "name", req.getName());
        }
        Department dept = Department.builder().organizationId(req.getOrganizationId())
                .name(req.getName()).code(req.getCode()).description(req.getDescription())
                .headEmployeeId(req.getHeadEmployeeId())
                .parentDepartmentId(req.getParentDepartmentId()).costCenter(req.getCostCenter())
                .email(req.getEmail()).phone(req.getPhone()).location(req.getLocation())
                .metadata(req.getMetadata()).active(true).build();
        Department saved = departmentRepository.save(dept);
        log.info("Department created: {} for org {}", saved.getName(), saved.getOrganizationId());
        return toResponse(saved);
    }

    public DepartmentResponse update(String id, DepartmentRequest req) {
        Department dept = findDept(id);
        dept.setName(req.getName()); dept.setCode(req.getCode());
        dept.setDescription(req.getDescription()); dept.setHeadEmployeeId(req.getHeadEmployeeId());
        dept.setParentDepartmentId(req.getParentDepartmentId()); dept.setCostCenter(req.getCostCenter());
        dept.setEmail(req.getEmail()); dept.setPhone(req.getPhone()); dept.setLocation(req.getLocation());
        dept.setMetadata(req.getMetadata());
        Department saved = departmentRepository.save(dept);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(String id) { return toResponse(findDept(id)); }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> getAll(String orgId, int page, int size, String search, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Department> deptPage;
        if (search != null)
            deptPage = departmentRepository.findByOrganizationIdAndNameContainingIgnoreCase(orgId, search, pageable);
        else if (active != null)
            deptPage = departmentRepository.findByOrganizationIdAndActive(orgId, active, pageable);
        else
            deptPage = departmentRepository.findByOrganizationId(orgId, pageable);
        return deptPage.map(this::toResponse);
    }

    public void delete(String id) { departmentRepository.delete(findDept(id)); log.info("Department deleted: {}", id); }

    public DepartmentResponse toggleActive(String id) {
        Department dept = findDept(id); dept.setActive(!dept.isActive());
        return toResponse(departmentRepository.save(dept));
    }

    private Department findDept(String id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder().id(d.getId()).organizationId(d.getOrganizationId())
                .name(d.getName()).code(d.getCode()).description(d.getDescription())
                .headEmployeeId(d.getHeadEmployeeId()).parentDepartmentId(d.getParentDepartmentId())
                .costCenter(d.getCostCenter()).active(d.isActive()).email(d.getEmail())
                .phone(d.getPhone()).location(d.getLocation()).metadata(d.getMetadata())
                .createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
