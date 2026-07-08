package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.TriggerRequest;
import com.aiflow.enterprise.dto.response.TriggerResponse;
import org.springframework.data.domain.Page;

public interface TriggerService {

    TriggerResponse createTrigger(TriggerRequest request);

    TriggerResponse getTriggerById(String id);

    Page<TriggerResponse> getAllTriggers(int page, int size, String workflowId, String type, Boolean active);

    TriggerResponse updateTrigger(String id, TriggerRequest request);

    void deleteTrigger(String id);

    TriggerResponse activateTrigger(String id);

    TriggerResponse deactivateTrigger(String id);
}
