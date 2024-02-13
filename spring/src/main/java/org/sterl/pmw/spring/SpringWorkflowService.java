package org.sterl.pmw.spring;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.sterl.pmw.boundary.AbstractWorkflowService;
import org.sterl.pmw.component.WorkflowRepository;
import org.sterl.pmw.model.Workflow;
import org.sterl.pmw.model.WorkflowId;
import org.sterl.pmw.model.WorkflowState;
import org.sterl.pmw.model.WorkflowStatus;
import org.sterl.pmw.spring.component.SpringWorkflowExecutorComponent;
import org.sterl.pmw.spring.model.PersistentWorkflowState;
import org.sterl.pmw.spring.repository.PersistentWorkflowStateRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class SpringWorkflowService extends AbstractWorkflowService<String> {

    private final SpringWorkflowExecutorComponent springWorkflowExecutor;
    private final PersistentWorkflowStateRepository workflowStateRepository;
    private final ObjectMapper mapper;

    public SpringWorkflowService(@NonNull WorkflowRepository workflowRepository, 
            PersistentWorkflowStateRepository workflowStateRepository,
            SpringWorkflowExecutorComponent springWorkflowExecutor,
            ObjectMapper mapper) {
        super(workflowRepository);
        this.workflowStateRepository = workflowStateRepository;
        this.springWorkflowExecutor = springWorkflowExecutor;
        this.mapper = mapper;
    }
    
    @Override
    public <T extends WorkflowState> WorkflowId execute(Workflow<T> workflow, T state, Duration delay) {
        
        PersistentWorkflowState persistentState = new PersistentWorkflowState();
        persistentState.setId(WorkflowId.newWorkflowId(persistentState.getCreated()));
        try {
            persistentState.setUserState(mapper.writeValueAsString(state));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        persistentState.setName(workflow.getName());
        workflowStateRepository.save(persistentState);

        log.debug("Starting workflow id={} name={} in delay={}", persistentState.getId(), workflow.getName(), delay);
        
        return persistentState.getId();
    }
    @Override
    public WorkflowStatus status(WorkflowId workflowId) {
        var w = workflowStateRepository.findById(workflowId);
        if (w.isEmpty()) return WorkflowStatus.COMPLETE;
        return w.get().getStatus();
    }
    @Override
    public void cancel(WorkflowId workflowId) {
        springWorkflowExecutor.cancel(workflowId);
    }
    @Override
    public int cancelAll() {
        return workflowStateRepository.setStatusFromStatus(WorkflowStatus.CANCELED,
                WorkflowStatus.ACTIVE_STATE, OffsetDateTime.now());
    }
    @Override
    public <T extends WorkflowState> String register(Workflow<T> workflow) throws IllegalArgumentException {
        return workflowRepository.register(workflow);
    }
}
