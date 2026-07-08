package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Organization;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.OrganizationRequest;
import com.aiflow.enterprise.org.dto.response.OrganizationResponse;
import com.aiflow.enterprise.org.repository.OrganizationRepository;
import com.aiflow.enterprise.org.repository.OrganizationSettingRepository;
import com.aiflow.enterprise.entity.OrganizationSetting;
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
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository orgRepository;
    private final OrganizationSettingRepository settingRepository;

    public OrganizationService(OrganizationRepository orgRepository,
                               OrganizationSettingRepository settingRepository) {
        this.orgRepository = orgRepository;
        this.settingRepository = settingRepository;
    }

    public OrganizationResponse create(OrganizationRequest req, String createdBy) {
        if (orgRepository.existsByName(req.getName())) {
            throw new DuplicateResourceException("Organization", "name", req.getName());
        }
        Organization org = Organization.builder()
                .name(req.getName()).legalName(req.getLegalName())
                .registrationNumber(req.getRegistrationNumber()).taxId(req.getTaxId())
                .email(req.getEmail()).phone(req.getPhone()).website(req.getWebsite())
                .addressLine1(req.getAddressLine1()).addressLine2(req.getAddressLine2())
                .city(req.getCity()).state(req.getState()).country(req.getCountry())
                .postalCode(req.getPostalCode()).description(req.getDescription())
                .industry(req.getIndustry()).domains(req.getDomains())
                .metadata(req.getMetadata()).createdBy(createdBy)
                .employeeCount(0).active(true).build();
        Organization saved = orgRepository.save(org);
        initSettings(saved.getId());
        log.info("Organization created: {} with id {}", saved.getName(), saved.getId());
        return toResponse(saved);
    }

    public OrganizationResponse update(String id, OrganizationRequest req) {
        Organization org = findOrg(id);
        if (!org.getName().equals(req.getName()) && orgRepository.existsByName(req.getName())) {
            throw new DuplicateResourceException("Organization", "name", req.getName());
        }
        org.setName(req.getName()); org.setLegalName(req.getLegalName());
        org.setRegistrationNumber(req.getRegistrationNumber()); org.setTaxId(req.getTaxId());
        org.setEmail(req.getEmail()); org.setPhone(req.getPhone()); org.setWebsite(req.getWebsite());
        org.setAddressLine1(req.getAddressLine1()); org.setAddressLine2(req.getAddressLine2());
        org.setCity(req.getCity()); org.setState(req.getState()); org.setCountry(req.getCountry());
        org.setPostalCode(req.getPostalCode()); org.setDescription(req.getDescription());
        org.setIndustry(req.getIndustry()); org.setDomains(req.getDomains());
        org.setMetadata(req.getMetadata());
        Organization saved = orgRepository.save(org);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getById(String id) {
        return toResponse(findOrg(id));
    }

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getAll(int page, int size, String search, String industry, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Organization> orgPage;
        if (search != null) orgPage = orgRepository.findByNameContainingIgnoreCase(search, pageable);
        else if (industry != null) orgPage = orgRepository.findByIndustry(industry, pageable);
        else if (active != null) orgPage = orgRepository.findByActive(active, pageable);
        else orgPage = orgRepository.findAll(pageable);
        return orgPage.map(this::toResponse);
    }

    public void delete(String id) {
        orgRepository.delete(findOrg(id));
        settingRepository.deleteByOrganizationId(id);
        log.info("Organization deleted: {}", id);
    }

    public OrganizationResponse toggleActive(String id) {
        Organization org = findOrg(id);
        org.setActive(!org.isActive());
        Organization saved = orgRepository.save(org);
        log.info("Organization {} active status set to: {}", id, saved.isActive());
        return toResponse(saved);
    }

    private void initSettings(String orgId) {
        settingRepository.findByOrganizationId(orgId).orElseGet(() -> {
            OrganizationSetting s = OrganizationSetting.builder()
                    .organizationId(orgId).allowSelfRegistration(true)
                    .requireEmailVerification(true).requireAdminApprovalForNewUsers(false)
                    .defaultTimezone("UTC").defaultLocale("en-US").dateFormat("MM/dd/yyyy")
                    .timeFormat("HH:mm").startOfWeek("monday").maxFailedLoginAttempts(5)
                    .passwordExpiryDays(90).sessionTimeoutMinutes(480)
                    .enableTwoFactorAuth(false).enableAuditLogging(true).build();
            return settingRepository.save(s);
        });
    }

    private Organization findOrg(String id) {
        return orgRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    private OrganizationResponse toResponse(Organization o) {
        return OrganizationResponse.builder().id(o.getId()).name(o.getName())
                .legalName(o.getLegalName()).registrationNumber(o.getRegistrationNumber())
                .taxId(o.getTaxId()).email(o.getEmail()).phone(o.getPhone())
                .website(o.getWebsite()).addressLine1(o.getAddressLine1())
                .addressLine2(o.getAddressLine2()).city(o.getState())
                .state(o.getState()).country(o.getCountry())
                .postalCode(o.getPostalCode()).logoUrl(o.getLogoUrl())
                .description(o.getDescription()).industry(o.getIndustry())
                .employeeCount(o.getEmployeeCount()).active(o.isActive())
                .createdBy(o.getCreatedBy()).domains(o.getDomains())
                .metadata(o.getMetadata()).createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt()).build();
    }
}
