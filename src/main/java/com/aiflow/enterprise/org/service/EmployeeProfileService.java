package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Department;
import com.aiflow.enterprise.entity.Designation;
import com.aiflow.enterprise.entity.EmployeeProfile;
import com.aiflow.enterprise.entity.Team;
import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.enums.EmployeeStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.EmployeeProfileRequest;
import com.aiflow.enterprise.org.dto.response.EmployeeProfileResponse;
import com.aiflow.enterprise.org.repository.DepartmentRepository;
import com.aiflow.enterprise.org.repository.DesignationRepository;
import com.aiflow.enterprise.org.repository.EmployeeProfileRepository;
import com.aiflow.enterprise.org.repository.TeamRepository;
import com.aiflow.enterprise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EmployeeProfileService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeProfileService.class);

    private final EmployeeProfileRepository empRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final DesignationRepository designationRepository;

    public EmployeeProfileService(EmployeeProfileRepository empRepository,
                                  UserRepository userRepository,
                                  DepartmentRepository departmentRepository,
                                  TeamRepository teamRepository,
                                  DesignationRepository designationRepository) {
        this.empRepository = empRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.designationRepository = designationRepository;
    }

    public EmployeeProfileResponse create(EmployeeProfileRequest req) {
        if (empRepository.findByUserId(req.getUserId()).isPresent()) {
            throw new DuplicateResourceException("EmployeeProfile", "userId", req.getUserId());
        }
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getUserId()));

        String employeeId = req.getEmployeeId() != null ? req.getEmployeeId() : generateEmployeeId(req.getOrganizationId());
        if (empRepository.findByOrganizationIdAndEmployeeId(req.getOrganizationId(), employeeId).isPresent()) {
            throw new DuplicateResourceException("EmployeeProfile", "employeeId", employeeId);
        }

        EmployeeProfile emp = EmployeeProfile.builder()
                .userId(req.getUserId()).organizationId(req.getOrganizationId())
                .employeeId(employeeId).departmentId(req.getDepartmentId())
                .teamIds(req.getTeamIds()).businessUnitIds(req.getBusinessUnitIds())
                .designationId(req.getDesignationId())
                .reportingManagerId(req.getReportingManagerId())
                .status(req.getStatus() != null ? req.getStatus() : EmployeeStatus.ONBOARDING)
                .employmentType(req.getEmploymentType())
                .joiningDate(req.getJoiningDate() != null ? req.getJoiningDate() : LocalDate.now())
                .workEmail(req.getWorkEmail() != null ? req.getWorkEmail() : user.getEmail())
                .workPhone(req.getWorkPhone()).extension(req.getExtension())
                .officeLocation(req.getOfficeLocation())
                .firstName(req.getFirstName() != null ? req.getFirstName() : user.getFirstName())
                .lastName(req.getLastName() != null ? req.getLastName() : user.getLastName())
                .phone(req.getPhone()).personalEmail(req.getPersonalEmail())
                .dateOfBirth(req.getDateOfBirth()).gender(req.getGender())
                .maritalStatus(req.getMaritalStatus()).nationality(req.getNationality())
                .addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .city(req.getCity()).state(req.getState()).country(req.getCountry())
                .postalCode(req.getPostalCode())
                .emergencyContactName(req.getEmergencyContactName())
                .emergencyContactPhone(req.getEmergencyContactPhone())
                .emergencyContactRelation(req.getEmergencyContactRelation())
                .bio(req.getBio()).skills(req.getSkills()).customFields(req.getCustomFields())
                .build();

        enrichRelations(emp);
        EmployeeProfile saved = empRepository.save(emp);
        log.info("Employee profile created: {} for user {}", saved.getEmployeeId(), saved.getUserId());
        return toResponse(saved);
    }

    public EmployeeProfileResponse update(String id, EmployeeProfileRequest req) {
        EmployeeProfile emp = findEmp(id);
        emp.setDepartmentId(req.getDepartmentId()); emp.setTeamIds(req.getTeamIds());
        emp.setBusinessUnitIds(req.getBusinessUnitIds()); emp.setDesignationId(req.getDesignationId());
        emp.setReportingManagerId(req.getReportingManagerId());
        emp.setEmploymentType(req.getEmploymentType()); emp.setWorkEmail(req.getWorkEmail());
        emp.setWorkPhone(req.getWorkPhone()); emp.setExtension(req.getExtension());
        emp.setOfficeLocation(req.getOfficeLocation());
        emp.setFirstName(req.getFirstName()); emp.setLastName(req.getLastName());
        emp.setPhone(req.getPhone()); emp.setPersonalEmail(req.getPersonalEmail());
        emp.setDateOfBirth(req.getDateOfBirth()); emp.setGender(req.getGender());
        emp.setMaritalStatus(req.getMaritalStatus()); emp.setNationality(req.getNationality());
        emp.setAddressLine1(req.getAddressLine1()); emp.setAddressLine2(req.getAddressLine2());
        emp.setCity(req.getCity()); emp.setState(req.getState()); emp.setCountry(req.getCountry());
        emp.setPostalCode(req.getPostalCode());
        emp.setEmergencyContactName(req.getEmergencyContactName());
        emp.setEmergencyContactPhone(req.getEmergencyContactPhone());
        emp.setEmergencyContactRelation(req.getEmergencyContactRelation());
        emp.setBio(req.getBio()); emp.setSkills(req.getSkills()); emp.setCustomFields(req.getCustomFields());
        enrichRelations(emp);
        return toResponse(empRepository.save(emp));
    }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getById(String id) { return toResponse(findEmp(id)); }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getByUserId(String userId) {
        return empRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeProfile", "userId", userId));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeProfileResponse> getAll(String orgId, int page, int size,
                                                 String deptId, String status, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "firstName"));
        Page<EmployeeProfile> empPage;

        if (deptId != null) {
            empPage = empRepository.findByDepartmentId(deptId, pageable);
        } else if (status != null) {
            empPage = empRepository.findByOrganizationIdAndStatus(orgId,
                    EmployeeStatus.valueOf(status.toUpperCase()), pageable);
        } else if (search != null) {
            empPage = empRepository.findAll(pageable);
        } else {
            empPage = empRepository.findByOrganizationId(orgId, pageable);
        }
        return empPage.map(this::toResponse);
    }

    public EmployeeProfileResponse updateStatus(String id, String status) {
        EmployeeProfile emp = findEmp(id);
        EmployeeStatus newStatus = EmployeeStatus.valueOf(status.toUpperCase());
        emp.setStatus(newStatus);
        if (newStatus == EmployeeStatus.TERMINATED) {
            emp.setExitDate(LocalDate.now());
        }
        EmployeeProfile saved = empRepository.save(emp);
        log.info("Employee {} status changed to: {}", saved.getEmployeeId(), newStatus);
        return toResponse(saved);
    }

    public EmployeeProfileResponse updateProfileImage(String id, String imageUrl) {
        EmployeeProfile emp = findEmp(id);
        emp.setProfileImageUrl(imageUrl);
        return toResponse(empRepository.save(emp));
    }

    public void delete(String id) { empRepository.delete(findEmp(id)); }

    private void enrichRelations(EmployeeProfile emp) {
        if (emp.getDepartmentId() != null) {
            departmentRepository.findById(emp.getDepartmentId()).ifPresent(d -> {
                emp.setDepartmentName(d.getName());
            });
        }
        if (emp.getDesignationId() != null) {
            designationRepository.findById(emp.getDesignationId()).ifPresent(d -> {
                emp.setDesignationTitle(d.getTitle());
            });
        }
        if (emp.getReportingManagerId() != null) {
            empRepository.findById(emp.getReportingManagerId()).ifPresent(m -> {
                emp.setReportingManagerName(m.getFirstName() + " " + m.getLastName());
            });
        }
        if (emp.getTeamIds() != null) {
            List<String> names = emp.getTeamIds().stream()
                    .map(tid -> teamRepository.findById(tid).map(Team::getName).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            emp.setTeamNames(names.isEmpty() ? null : names);
        }
    }

    private String generateEmployeeId(String orgId) {
        long count = empRepository.countByOrganizationId(orgId);
        return "EMP-" + (count + 1);
    }

    private EmployeeProfile findEmp(String id) {
        return empRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeProfile", "id", id));
    }

    private EmployeeProfileResponse toResponse(EmployeeProfile e) {
        return EmployeeProfileResponse.builder()
                .id(e.getId()).userId(e.getUserId()).organizationId(e.getOrganizationId())
                .employeeId(e.getEmployeeId()).departmentId(e.getDepartmentId())
                .departmentName(e.getDepartmentName()).teamIds(e.getTeamIds())
                .teamNames(e.getTeamNames()).businessUnitIds(e.getBusinessUnitIds())
                .designationId(e.getDesignationId()).designationTitle(e.getDesignationTitle())
                .reportingManagerId(e.getReportingManagerId())
                .reportingManagerName(e.getReportingManagerName())
                .status(e.getStatus()).employmentType(e.getEmploymentType())
                .joiningDate(e.getJoiningDate()).exitDate(e.getExitDate())
                .exitReason(e.getExitReason()).workEmail(e.getWorkEmail())
                .workPhone(e.getWorkPhone()).extension(e.getExtension())
                .officeLocation(e.getOfficeLocation()).firstName(e.getFirstName())
                .lastName(e.getLastName()).phone(e.getPhone()).personalEmail(e.getPersonalEmail())
                .dateOfBirth(e.getDateOfBirth()).gender(e.getGender())
                .maritalStatus(e.getMaritalStatus()).nationality(e.getNationality())
                .addressLine1(e.getAddressLine1()).addressLine2(e.getAddressLine2())
                .city(e.getCity()).state(e.getState()).country(e.getCountry())
                .postalCode(e.getPostalCode())
                .emergencyContactName(e.getEmergencyContactName())
                .emergencyContactPhone(e.getEmergencyContactPhone())
                .emergencyContactRelation(e.getEmergencyContactRelation())
                .profileImageUrl(e.getProfileImageUrl()).bio(e.getBio())
                .skills(e.getSkills()).education(e.getEducation())
                .certifications(e.getCertifications()).customFields(e.getCustomFields())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build();
    }
}
