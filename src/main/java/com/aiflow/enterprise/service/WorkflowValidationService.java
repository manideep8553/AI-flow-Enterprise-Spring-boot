package com.aiflow.enterprise.service;

import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowValidationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowValidationService.class);

    public ValidationResult validate(List<WorkflowStep> steps) {
        ValidationResult result = new ValidationResult();
        if (steps == null || steps.isEmpty()) {
            result.addError("Workflow must have at least one step");
            return result;
        }

        Set<String> ids = new HashSet<>();
        Map<String, WorkflowStep> stepMap = new HashMap<>();

        for (WorkflowStep step : steps) {
            if (step.getStepId() == null || step.getStepId().isBlank()) {
                result.addError("Step missing ID");
                continue;
            }
            if (!ids.add(step.getStepId())) {
                result.addError("Duplicate step ID: " + step.getStepId());
            }
            if (step.getName() == null || step.getName().isBlank()) {
                result.addError("Step '" + step.getStepId() + "' missing name");
            }
            if (step.getType() == null) {
                result.addError("Step '" + step.getName() + "' missing type");
            }
            stepMap.put(step.getStepId(), step);
        }

        for (WorkflowStep step : steps) {
            if (step.getDependsOn() != null) {
                for (String dep : step.getDependsOn()) {
                    if (!stepMap.containsKey(dep)) {
                        result.addError("Step '" + step.getName()
                                + "' depends on non-existent step: " + dep);
                    }
                }
            }
        }

        if (hasCycle(steps)) {
            result.addError("Workflow contains circular dependencies (cycle detected)");
        }

        if (result.hasErrors()) {
            log.warn("Workflow validation failed with {} errors", result.getErrors().size());
        } else {
            log.info("Workflow validation passed for {} steps", steps.size());
        }

        return result;
    }

    private boolean hasCycle(List<WorkflowStep> steps) {
        Map<String, List<String>> graph = new HashMap<>();
        for (WorkflowStep s : steps) {
            graph.put(s.getStepId(), s.getDependsOn() != null
                    ? new ArrayList<>(s.getDependsOn()) : new ArrayList<>());
        }
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        for (String node : graph.keySet()) {
            if (hasCycleDfs(node, graph, visited, recursionStack)) return true;
        }
        return false;
    }

    private boolean hasCycleDfs(String node, Map<String, List<String>> graph,
                                 Set<String> visited, Set<String> stack) {
        if (stack.contains(node)) return true;
        if (visited.contains(node)) return false;
        visited.add(node);
        stack.add(node);
        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (hasCycleDfs(neighbor, graph, visited, stack)) return true;
        }
        stack.remove(node);
        return false;
    }

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        public void addError(String error) { errors.add(error); }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean isValid() { return errors.isEmpty(); }
    }
}
