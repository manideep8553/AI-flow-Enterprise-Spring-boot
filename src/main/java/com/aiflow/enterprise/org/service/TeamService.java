package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Team;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.TeamRequest;
import com.aiflow.enterprise.org.dto.response.TeamResponse;
import com.aiflow.enterprise.org.repository.TeamRepository;
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
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) { this.teamRepository = teamRepository; }

    public TeamResponse create(TeamRequest req) {
        if (teamRepository.findByDepartmentIdAndName(req.getDepartmentId(), req.getName()).isPresent()) {
            throw new DuplicateResourceException("Team", "name", req.getName());
        }
        Team team = Team.builder().organizationId(req.getOrganizationId())
                .departmentId(req.getDepartmentId()).name(req.getName())
                .description(req.getDescription()).leadEmployeeId(req.getLeadEmployeeId())
                .email(req.getEmail()).slackChannel(req.getSlackChannel())
                .metadata(req.getMetadata()).active(true).build();
        Team saved = teamRepository.save(team);
        log.info("Team created: {} in dept {}", saved.getName(), saved.getDepartmentId());
        return toResponse(saved);
    }

    public TeamResponse update(String id, TeamRequest req) {
        Team team = findTeam(id);
        team.setName(req.getName()); team.setDescription(req.getDescription());
        team.setLeadEmployeeId(req.getLeadEmployeeId()); team.setEmail(req.getEmail());
        team.setSlackChannel(req.getSlackChannel()); team.setMetadata(req.getMetadata());
        return toResponse(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public TeamResponse getById(String id) { return toResponse(findTeam(id)); }

    @Transactional(readOnly = true)
    public Page<TeamResponse> getByDepartment(String deptId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Team> teamPage;
        if (search != null)
            teamPage = teamRepository.findByDepartmentIdAndNameContainingIgnoreCase(deptId, search, pageable);
        else
            teamPage = teamRepository.findByDepartmentId(deptId, pageable);
        return teamPage.map(this::toResponse);
    }

    public void delete(String id) { teamRepository.delete(findTeam(id)); log.info("Team deleted: {}", id); }

    private Team findTeam(String id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    private TeamResponse toResponse(Team t) {
        return TeamResponse.builder().id(t.getId()).organizationId(t.getOrganizationId())
                .departmentId(t.getDepartmentId()).name(t.getName())
                .description(t.getDescription()).leadEmployeeId(t.getLeadEmployeeId())
                .active(t.isActive()).email(t.getEmail()).slackChannel(t.getSlackChannel())
                .metadata(t.getMetadata()).createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt()).build();
    }
}
