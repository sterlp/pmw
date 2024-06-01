package org.sterl.pmw.spring;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.AbstractWorkflowService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.RunningWorkflowState;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.spring.component.SpringWorkflowExecutorComponent;
import org.sterl.pmw.spring.model.TaskEntity;
import org.sterl.pmw.spring.repository.TaskRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SpringWorkflowService  {

    private final SpringWorkflowExecutorComponent springWorkflowExecutor;
    private final TaskRepository workflowStateRepository;
    private final ObjectMapper mapper;

    public void trigger()
}
